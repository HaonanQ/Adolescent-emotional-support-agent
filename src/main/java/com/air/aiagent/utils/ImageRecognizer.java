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
     *
     * 改造点：直接使用腾讯云COS可访问URL，无需Base64编码
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

        // 流式响应接收器（适配Flux返回值）
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        // 新增：线程安全的响应内容拼接器
        StringBuffer aiResponseBuilder = new StringBuffer();

        // 异步执行SDK调用（避免阻塞主线程）
        new Thread(() -> {
            try {
                // ========== 核心改造：URL传参（阿里云官方格式） ==========
                String imageUrl = null;
                if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
                    // 1. 解析URL/对象路径，获取腾讯云COS可访问URL
                    if (request.getImageUrl().startsWith("https://") || request.getImageUrl().startsWith("http://")) {
                        imageUrl = request.getImageUrl(); // 直接使用可访问URL
                    } else {
                        // 从COS对象路径转换为可访问URL
                        String objectPath = cosManager.extractObjectName(request.getImageUrl());
                        imageUrl = cosManager.getFileUrl(objectPath);
                    }
                    log.info("腾讯云COS图片可访问URL：{}", imageUrl);
                }

                // ========== 1. 构建官方多模态消息（URL格式） ==========
                MultiModalConversation conv = new MultiModalConversation();
                List<Map<String, Object>> contentList = new ArrayList<>();

                // 1.1 添加图片URL（阿里云官方要求的格式）
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    contentList.add(Collections.singletonMap("image", imageUrl));
                }
                // 1.2 添加文本提示词
                contentList.add(Collections.singletonMap("text", prompt));

                // 1.3 构建用户消息（指定USER角色）
                MultiModalMessage userMsg = MultiModalMessage.builder()
                        .role(Role.USER.getValue())
                        .content(contentList)
                        .build();

                // ========== 2. 构建官方调用参数 ==========
                MultiModalConversationParam param = MultiModalConversationParam.builder()
                        .apiKey(apiKey)                  // 阿里云API Key
                        .model(imageModel)               // 多模态模型（必须是qwen3-vl-plus/qwen-vl-plus）
                        .message(userMsg)                // 图片URL + 文本消息
                        .temperature(temperature)        // 温度系数
                        .maxTokens(maxTokens)            // 最大令牌数
                        .incrementalOutput(true)         // 启用增量输出（流式）
                        .build();

                // ========== 3. 调用官方SDK（流式响应） ==========
                conv.streamCall(param)
                        .doOnNext(result -> {
                            // 解析流式响应内容
                            if (result != null && result.getOutput() != null
                                    && !result.getOutput().getChoices().isEmpty()) {
                                // 1. 获取原始响应文本
                                String rawContent = result.getOutput().getChoices().get(0).getMessage().getContent().toString();
                                if (rawContent != null && !rawContent.isEmpty()) {
                                    // 2. 清洗格式：移除方括号、{text=} 包裹、
                                    String cleanContent = rawContent
                                            .replaceAll("\\[|\\]", "") // 移除所有方括号
                                            .replaceAll("\\{text=", "") // 移除 {text= 前缀
                                            .replaceAll("}", ""); // 移除 } 后缀
                                    // 3. 仅拼接非空清洗后的内容
                                    if (!cleanContent.isEmpty()) {
                                        aiResponseBuilder.append(cleanContent); // 拼接清洗后的内容
                                        sink.tryEmitNext(cleanContent); // 发送清洗后的流式数据
                                    }
                                }
                            }
                        })
                        .doOnComplete(() -> {
                            // ========== 响应完成：保存消息 ==========
                            long duration = System.currentTimeMillis() - startTime;
                            // 1. 获取拼接后的完整文本
                            String aiContent = aiResponseBuilder.toString();

                            // 2. 空值兜底
                            if (aiContent.isEmpty()) {
                                aiContent = "未识别到有效内容";
                            }
                            // 保存成功消息
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
                            sink.tryEmitComplete(); // 结束流式响应
                        })
                        .doOnError(e -> {
                            // ========== 异常处理：保存失败消息 ==========
                            long duration = System.currentTimeMillis() - startTime;
                            String errorMsg = "图片分析失败：" + e.getMessage();
                            ChatMessage errorAiMessage = ChatMessage.builder()
                                    .id(aiMessageId)
                                    .chatId(request.getChatId())
                                    .sessionId(request.getSessionId())
                                    .content(errorMsg)
                                    .messageType(MessageType.TEXT)
                                    .isAiResponse(true)
                                    .metadata(MessageMetadata.builder()
                                            .responseTimeMs((int) duration)
                                            .tokenCount(0)
                                            .build())
                                    .build();
                            chatMessageService.save(errorAiMessage);
                            log.error("AI消息分析失败并保存，sessionId={}, 错误={}", request.getSessionId(), e.getMessage(), e);
                            chatSessionService.incrementMessageCount(request.getSessionId());
                            sink.tryEmitNext(errorMsg);
                            sink.tryEmitComplete();
                        })
                        .subscribe(); // 订阅流式响应

            } catch (Exception e) {
                // 全局异常捕获
                long duration = System.currentTimeMillis() - startTime;
                String errorMsg = "图片分析失败：" + e.getMessage();
                log.error("多模态模型调用全局异常", e);
                // 保存失败消息
                ChatMessage errorAiMessage = ChatMessage.builder()
                        .id(aiMessageId)
                        .chatId(request.getChatId())
                        .sessionId(request.getSessionId())
                        .content(errorMsg)
                        .messageType(MessageType.TEXT)
                        .isAiResponse(true)
                        .metadata(MessageMetadata.builder()
                                .responseTimeMs((int) duration)
                                .tokenCount(0)
                                .build())
                        .build();
                chatMessageService.save(errorAiMessage);
                chatSessionService.incrementMessageCount(request.getSessionId());
                sink.tryEmitNext(errorMsg);
                sink.tryEmitComplete();
            }
        }).start();
//        // ========== 核心改造：直接使用腾讯云COS可访问URL（移除Base64转换） ==========
//        String imageUrl = null;
//        if (request.getImageUrl() != null && !request.getImageUrl().isEmpty()) {
//            try {
//                // 验证URL有效性（可选：检查是否为腾讯云COS合法URL）
//                if (request.getImageUrl().startsWith("https://") || request.getImageUrl().startsWith("http://")) {
//                    imageUrl = request.getImageUrl(); // 直接使用可访问URL
//                    log.info("腾讯云COS图片可访问URL：{}", imageUrl);
//                } else {
//                    // 如果传入的是COS对象路径，转换为可访问URL
//                    String objectPath = cosManager.extractObjectName(request.getImageUrl());
//                    imageUrl = cosManager.getFileUrl(objectPath); // 调用CosManager获取可访问URL
//                }
//            } catch (Exception e) {
//                log.error("获取腾讯云COS图片URL失败", e);
//                return Flux.just("图片URL解析失败：" + e.getMessage());
//            }
//        }

//        // 创建 options 对象，指定使用 qwen 多模态模型
//        DashScopeChatOptions options = DashScopeChatOptions.builder()
//                .withModel(imageModel)      // 必须用qwen3-vl-plus/qwen-vl-plus（多模态模型）
//                .withTemperature(temperature2)
//                .withMaxToken(maxTokens)
//                .withMultiModel(true) // 启用多模态（关键）
//                .withIncrementalOutput(true) // 增量输出（流式必须）
//                .build();

//        // ========== 构建多模态提示词（直接传入图片URL） ==========
//        StringBuilder promptBuilder = new StringBuilder();
//        promptBuilder.append(prompt);
//        // 拼接图片URL（DashScope多模态支持直接解析公网URL）
//        if (imageUrl != null) {
//            promptBuilder.append("\n图片URL：").append(imageUrl);
//        }
//        String finalPrompt = promptBuilder.toString();
//        StringBuffer aiResponseBuilder = new StringBuffer();

//        // 流式调用（保留原有逻辑）
//        return imageClient.prompt()
//                .user(finalPrompt)
//                .options(options)
//                .stream() // 必须流式调用
//                .content()
//                .doOnNext(chunk -> {
//                    if (chunk != null) { // 空值防护
//                        aiResponseBuilder.append(chunk);
//                    }
//                })
//                .filter(Objects::nonNull)
//                // 异常处理（保留原有逻辑）
//                .onErrorResume(e -> {
//                    long duration = System.currentTimeMillis() - startTime;
//                    String errorMsg = "图片分析失败：" + e.getMessage();
//                    // 保存失败的AI消息
//                    ChatMessage errorAiMessage = ChatMessage.builder()
//                            .id(aiMessageId)
//                            .chatId(request.getChatId())
//                            .sessionId(request.getSessionId())
//                            .content(errorMsg)
//                            .messageType(MessageType.TEXT)
//                            .isAiResponse(true)
//                            .metadata(MessageMetadata.builder()
//                                    .responseTimeMs((int) duration)
//                                    .tokenCount(0)
//                                    .build())
//                            .build();
//                    chatMessageService.save(errorAiMessage);
//                    log.error("AI消息分析失败并保存，sessionId={}, 错误={}", request.getSessionId(), e.getMessage(), e);
//                    chatSessionService.incrementMessageCount(request.getSessionId());
//                    // 返回错误提示文本
//                    return Flux.just(errorMsg);
//                })
//                .doOnComplete(() -> {
//                    long duration = System.currentTimeMillis() - startTime;
//                    String aiContent = aiResponseBuilder.toString();
//                    // 空内容防护
//                    if (aiContent.isEmpty()) {
//                        aiContent = "未识别到有效内容";
//                    }
//                    ChatMessage aiMessage = ChatMessage.builder()
//                            .id(aiMessageId)
//                            .chatId(request.getChatId())
//                            .sessionId(request.getSessionId())
//                            .content(aiContent)
//                            .messageType(MessageType.TEXT)
//                            .isAiResponse(true)
//                            .metadata(MessageMetadata.builder()
//                                    .responseTimeMs((int) duration)
//                                    .tokenCount(estimateTokens(aiContent))
//                                    .build())
//                            .build();
//                    chatMessageService.save(aiMessage);
//                    log.info("AI 消息已保存，sessionId={}, 长度={}", request.getSessionId(), aiContent.length());
//                    chatSessionService.incrementMessageCount(request.getSessionId());
//                });

        // 返回流式响应
        return sink.asFlux().filter(Objects::nonNull);
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

}