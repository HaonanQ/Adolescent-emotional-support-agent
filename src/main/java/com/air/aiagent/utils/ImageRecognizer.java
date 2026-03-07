package com.air.aiagent.utils;

import cn.hutool.core.lang.UUID;
import com.air.aiagent.advisor.MyLoggerAdvisor;
import com.air.aiagent.domain.dto.ChatRequest;
import com.air.aiagent.domain.entity.ChatMessage;
import com.air.aiagent.domain.entity.MessageMetadata;
import com.air.aiagent.domain.entity.MessageType;
import com.air.aiagent.service.impl.ChatMessageService;
import com.air.aiagent.service.impl.ChatSessionService;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import com.air.aiagent.manage.CosManager;
import com.air.aiagent.exception.BusinessException;
import com.air.aiagent.exception.ErrorCode;
import jakarta.annotation.Resource;

import java.io.File;
import java.util.*;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import reactor.core.publisher.Sinks;

/**
 * 图像识别器
 */
@Component
@Slf4j
public class ImageRecognizer {

    private final ChatClient imageClient;
    @Resource
    private ChatMessageService chatMessageService;
    @Resource
    private ChatSessionService chatSessionService;
    @Value("${ai.image.model:qwen3.5-plus}")
    private String imageModel;

    @Value("${ai.image.temperature:0.1}")
    private Float temperature;
    @Value("${ai.image.temperature:0.1}")
    private Double temperature2;
    @Value("${ai.image.max-tokens:500}")
    private Integer maxTokens;
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;
    @Autowired // 注入已有的 CosManager
    private CosManager cosManager;
    /**
     * 注入图像识别专用的 ChatModel
     */
    public ImageRecognizer(@Qualifier("imageChatModel") ChatModel imageChatModel) {
        this.imageClient = ChatClient
                .builder(imageChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
    }
    /**
     * 识别具体场景（用于精准推荐）
     *
     * @param request 用户消息
     * @return ai对图片所表现出来的人物的情感分析
     */
    public Flux<String> recognizeScene(ChatRequest request) {
        log.info("已进入到该函数里面，用户消息：{}，图片URL：{}",
                request.getMessage(), request.getImageUrl());
        String prompt = buildIntentPrompt(request.getMessage());// 用户的提示文本拼接
        String aiMessageId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        /// 2. 读取本地MinIO图片并转Base64
        String imageBase64 = null;
        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
            try {
                String objectPath = cosManager.extractObjectName(request.getImageUrl());
                imageBase64 = cosManager.getImageBase64(objectPath);
            } catch (BusinessException e) {
                log.error("图片Base64转换失败", e);
                return Flux.just("图片解析失败：" + e.getMessage());
            }
        }
        // 创建 options 对象，指定使用 qwen 模型
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .withModel(imageModel)      // qwen3.5-plus
                .withTemperature(temperature2)  // 0.1
                .withMaxToken(maxTokens)    // 50
                .withMultiModel(true) // 启用多模态
                .withIncrementalOutput(true) // 关键：启用增量输出（模型强制要求）
                .build();
        // 3. 构建多模态消息（Spring AI方式，无需Media类的替代方案）
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(prompt);
        // 把Base64图片拼接到提示词中（Spring AI兼容方式）
        if (imageBase64 != null) {
            promptBuilder.append("\n图片Base64：").append(imageBase64);
        }
        String finalPrompt = promptBuilder.toString();
        StringBuffer aiResponseBuilder = new StringBuffer();
        // 5. 仅使用流式调用（同步call()会报错，模型仅支持流式）
        return imageClient.prompt()
                .user(finalPrompt)
                .options(options)
                .stream() // 必须流式调用，不能用call()
                .content()
                .doOnNext(chunk -> {
                    if (chunk != null) { // 空值防护
                        aiResponseBuilder.append(chunk);
                    }
                })
                .filter(Objects::nonNull)
                // 3. 修复：异常时也保存消息（标记为失败）
                .onErrorResume(e -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String errorMsg = "图片分析失败：" + e.getMessage();
                    // 保存失败的AI消息
                    ChatMessage errorAiMessage = ChatMessage.builder()
                            .id(aiMessageId)
                            .chatId(request.getChatId())
                            .sessionId(request.getSessionId())
                            .content(errorMsg)
                            .messageType(MessageType.TEXT)
                            .isAiResponse(true)
                            .metadata(MessageMetadata.builder()
                                    .responseTimeMs((int) duration)
                                    .tokenCount(0) // 失败时token数为0
                                    .build())
                            .build();
                    chatMessageService.save(errorAiMessage);
                    log.error("AI消息分析失败并保存，sessionId={}, 错误={}", request.getSessionId(), e.getMessage(), e);
                    chatSessionService.incrementMessageCount(request.getSessionId());
                    // 返回错误提示文本
                    return Flux.just(errorMsg);
                })
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String aiContent = aiResponseBuilder.toString();
                    // 空内容防护
                    if (aiContent.isEmpty()) {
                        aiContent = "未识别到有效内容";
                    }
                    ChatMessage aiMessage = ChatMessage.builder()
                            .id(aiMessageId)
                            .chatId(request.getChatId())
                            .sessionId(request.getSessionId())
                            .content(aiContent)
                            .messageType(MessageType.TEXT)
                            .isAiResponse(true)
                            .metadata(MessageMetadata.builder()
                                    .responseTimeMs((int) duration)
                                    .tokenCount(estimateTokens(aiContent))
                                    .build())
                            .build();
                    chatMessageService.save(aiMessage);
                    log.info("AI 消息已保存，sessionId={}, 长度={}", request.getSessionId(), aiContent.length());
                    chatSessionService.incrementMessageCount(request.getSessionId());
                });

          // 3. 构建官方多模态消息（完全绕开Spring AI）
//        MultiModalConversation conv = new MultiModalConversation();
//        List<Map<String, Object>> contentList = new ArrayList<>();
//
//        // 3.1 添加Base64图片（官方格式）
//        if (imageBase64 != null) {
//            contentList.add(Collections.singletonMap("image", imageBase64));
//        }
//        // 3.2 添加文本提示词
//        contentList.add(Collections.singletonMap("text", prompt));
//
//        // 3.3 构建用户消息
//        MultiModalMessage userMsg = MultiModalMessage.builder()
//                .role(Role.USER.getValue())
//                .content(contentList)
//                .build();
//
//        // 4. 构建官方调用参数（指定多模态模型）
//        MultiModalConversationParam param = MultiModalConversationParam.builder()
//                .apiKey(apiKey)
//                .model(imageModel)               // 多模态模型（qwen3-vl-plus）
//                .message(userMsg)                // 图片+文本消息
//                .temperature(temperature)        // 温度系数
//                .maxTokens(maxTokens)            // 最大令牌数
//                .build();
//        // 5. 调用官方API并返回结果
//        try {
//            MultiModalConversationResult result = conv.call(param);
//            // 解析响应结果
//            String responseText = result.getOutput().getChoices().get(0).getMessage().getContent().toString();
//            log.info("图片分析结果：{}", responseText);
//            return Flux.just(responseText);
//        } catch (ApiException | NoApiKeyException | UploadFileException e) {
//            log.error("多模态模型调用失败", e);
//            return Flux.just("图片分析失败：" + e.getMessage());
//        }
    }

    /**
     * 图像意图识别提示词
     */
    private String buildIntentPrompt(String userMessage) {
        return """
           你是一名专业的青少年情感分析专家，擅长通过图片分析人物的情绪状态。
           分析规则：
           1. 如果图片中没有人出现，直接回答“该图片没有人物，请换一张图片”；
           2. 如果有人物，精准描述人物的情感（如开心、难过、焦虑、愤怒、平静等）；
           用户的问题：%s
           """.formatted(userMessage);
    }
    private int estimateTokens(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.length() / 3;
    }
//    /**
//     * 识别图片中人物情感（改用官方SDK + 本地文件路径）
//     */
//    /**
//     * 识别图片中人物情感（改用官方SDK + 本地文件路径）
//     */
//    public Flux<String> recognizeScene(ChatRequest request) {
//        log.info("已进入到该函数里面，用户消息：{}，图片URL：{}",
//                request.getMessage(), request.getImageUrl());
//
//        // 初始化核心变量（保留你原有逻辑）
//        String prompt = buildIntentPrompt(request.getMessage());
//        String aiMessageId = UUID.randomUUID().toString();
//        long startTime = System.currentTimeMillis();
//        // 新增：线程安全的字符串拼接器（替代sink获取buffer）
//        StringBuffer aiResponseBuilder = new StringBuffer();
//        // 流式响应接收器（适配Flux返回值）
//        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
//
//        // 异步执行（避免阻塞主线程）
//        new Thread(() -> {
//            File localImageFile = null;
//            try {
//                // ========== 步骤 1：调用你已有的 CosManager 下载图片到本地 tmpimage ==========
//                if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
//                    String objectPath = cosManager.extractObjectName(request.getImageUrl());
//                    localImageFile = cosManager.downloadImageToTmp(objectPath); // 直接复用你的下载方法
//                }
//
//                // ========== 步骤2：构建官方多模态消息（file://本地路径） ==========
//                List<Map<String, Object>> contentList = new ArrayList<>();
//                // 1. 添加本地图片（file://协议）
//                if (localImageFile != null) {
//                    String filePath = "file://" + localImageFile.getAbsolutePath();
//                    contentList.add(new HashMap<String, Object>() {{
//                        put("image", filePath);
//                    }});
//                }
//                // 2. 添加文本提示词
//                contentList.add(new HashMap<String, Object>() {{
//                    put("text", prompt);
//                }});
//
//                // ========== 步骤3：构建官方调用参数 ==========
//                MultiModalMessage userMessage = MultiModalMessage.builder()
//                        .role(Role.USER.getValue())
//                        .content(contentList)
//                        .build();
//
//                MultiModalConversationParam param = MultiModalConversationParam.builder()
//                        .apiKey(apiKey) // 从配置读取API Key
//                        .model(imageModel)
//                        .messages(Collections.singletonList(userMessage))
//                        .temperature(temperature)
//                        .maxTokens(maxTokens)
//                        .incrementalOutput(true) // 启用增量输出（流式）
//                        .build();
//
//                // ========== 步骤4：流式调用（完善异常处理+空值防护） ==========
//                MultiModalConversation conv = new MultiModalConversation();
//                conv.streamCall(param)
//                        // 添加全局异常处理器（解决OnErrorNotImplementedException）
//                        .onErrorReturn(e -> {
//                            log.error("模型流式调用异常", e);
//                            return null; // 异常时返回null，后续过滤
//                        })
//                        // 空值过滤
//                        .filter(Objects::nonNull)
//                        .doOnNext(result -> {
//                            try {
//                                // ========== 核心修复：数组越界防护 ==========
//                                // 1. 检查choices列表是否为空
//                                if (result.getOutput() == null ||
//                                        result.getOutput().getChoices() == null ||
//                                        result.getOutput().getChoices().isEmpty()) {
//                                    log.warn("模型返回choices为空");
//                                    return;
//                                }
//                                // 2. 检查message内容是否为空
//                                var message = result.getOutput().getChoices().get(0).getMessage();
//                                if (message == null || message.getContent() == null || message.getContent().isEmpty()) {
//                                    log.warn("模型返回content为空");
//                                    return;
//                                }
//                                // 3. 安全获取text内容
//                                Map<String, Object> contentMap = message.getContent().get(0);
//                                Object textObj = contentMap.get("text");
//                                if (textObj != null) {
//                                    String content = textObj.toString().trim();
//                                    if (!content.isEmpty()) {
//                                        aiResponseBuilder.append(content);
//                                        sink.tryEmitNext(content);
//                                    }
//                                }
//                            } catch (IndexOutOfBoundsException e) {
//                                log.error("模型响应解析数组越界", e);
//                                // 发送友好提示，不中断流程
//                                sink.tryEmitNext("图片分析失败：模型返回数据格式异常");
//                            } catch (Exception e) {
//                                log.error("模型响应解析异常", e);
//                            }
//                        })
//                        .doOnComplete(() -> {
//                            // 响应完成处理
//                            long duration = System.currentTimeMillis() - startTime;
//                            String aiContent = aiResponseBuilder.toString().trim();
//                            if (aiContent.isEmpty()) {
//                                aiContent = "未识别到有效内容";
//                                sink.tryEmitNext(aiContent);
//                            }
//                            saveSuccessMessage(aiMessageId, request, aiContent, duration);
//                            sink.tryEmitComplete();
//                        })
//                        // 订阅时指定异常处理器（最终兜底）
//                        .subscribe(
//                                null, // onNext已在doOnNext处理
//                                e -> {
//                                    log.error("订阅异常", e);
//                                    long duration = System.currentTimeMillis() - startTime;
//                                    String errorMsg = "图片分析失败：" + e.getMessage();
//                                    saveFailMessage(aiMessageId, request, errorMsg, duration);
//                                    sink.tryEmitNext(errorMsg);
//                                    sink.tryEmitComplete();
//                                },
//                                () -> log.info("模型流式调用完成") // onComplete
//                        );
//
//            } catch (BusinessException e) {
//                // MinIO业务异常
//                long duration = System.currentTimeMillis() - startTime;
//                String errorMsg = "图片下载失败：" + e.getMessage();
//                log.error(errorMsg, e);
//                saveFailMessage(aiMessageId, request, errorMsg, duration);
//                sink.tryEmitNext(errorMsg);
//                sink.tryEmitComplete();
//            } catch (Exception e) {
//                // 全局异常
//                long duration = System.currentTimeMillis() - startTime;
//                String errorMsg = "图片分析失败：" + e.getMessage();
//                log.error(errorMsg, e);
//                saveFailMessage(aiMessageId, request, errorMsg, duration);
//                sink.tryEmitNext(errorMsg);
//                sink.tryEmitComplete();
//            } finally {
//                // 清理临时文件
//                if (localImageFile != null) {
//                    cosManager.deleteTempFile(localImageFile);
//                }
//            }
//        });
//
//        // 返回流式响应
//        return sink.asFlux().filter(Objects::nonNull);
//    }
//
//    /**
//     * 保存成功的AI消息（完全复用你原有逻辑）
//     */
//    private void saveSuccessMessage(String aiMessageId, ChatRequest request, String aiContent, long duration) {
//        ChatMessage aiMessage = ChatMessage.builder()
//                .id(aiMessageId)
//                .chatId(request.getChatId())
//                .sessionId(request.getSessionId())
//                .content(aiContent)
//                .messageType(MessageType.TEXT)
//                .isAiResponse(true)
//                .metadata(MessageMetadata.builder()
//                        .responseTimeMs((int) duration)
//                        .tokenCount(estimateTokens(aiContent))
//                        .build())
//                .build();
//        chatMessageService.save(aiMessage);
//        chatSessionService.incrementMessageCount(request.getSessionId());
//        log.info("AI 消息已保存，sessionId={}, 长度={}", request.getSessionId(), aiContent.length());
//    }
//
//    /**
//     * 保存失败的AI消息（完全复用你原有逻辑）
//     */
//    private void saveFailMessage(String aiMessageId, ChatRequest request, String errorMsg, long duration) {
//        ChatMessage errorAiMessage = ChatMessage.builder()
//                .id(aiMessageId)
//                .chatId(request.getChatId())
//                .sessionId(request.getSessionId())
//                .content(errorMsg)
//                .messageType(MessageType.TEXT)
//                .isAiResponse(true)
//                .metadata(MessageMetadata.builder()
//                        .responseTimeMs((int) duration)
//                        .tokenCount(0)
//                        .build())
//                .build();
//        chatMessageService.save(errorAiMessage);
//        chatSessionService.incrementMessageCount(request.getSessionId());
//        log.error("AI消息分析失败并保存，sessionId={}, 错误={}", request.getSessionId(), errorMsg);
//    }
}