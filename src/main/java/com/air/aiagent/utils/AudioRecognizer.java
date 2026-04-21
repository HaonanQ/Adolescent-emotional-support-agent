package com.air.aiagent.utils;

import cn.hutool.core.lang.UUID;
import com.air.aiagent.advisor.MyLoggerAdvisor;
import com.air.aiagent.domain.dto.ChatRequest;
import com.air.aiagent.domain.entity.ChatMessage;
import com.air.aiagent.domain.entity.MessageMetadata;
import com.air.aiagent.domain.entity.MessageType;
import com.air.aiagent.service.impl.ChatMessageService;
import com.air.aiagent.service.impl.ChatSessionService;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import jakarta.annotation.Resource;

import java.util.*;

/**
 * 音频识别器
 */
@Component
@Slf4j
public class AudioRecognizer {

    @Resource
    private ChatMessageService chatMessageService;
    @Resource
    private ChatSessionService chatSessionService;
    
    @Value("${ai.audio.model}")
    private String audioModel;
    
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    /**
     * 识别音频并进行智能对话
     * @param request 聊天请求
     * @return AI的流式响应
     */
    public Flux<String> recognizeAudio(ChatRequest request) {
        log.info("音频识别，音频URL：{}", request.getAudioUrl());
        String aiMessageId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        // 流式响应接收器
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        StringBuffer aiResponseBuilder = new StringBuffer();

        // 异步执行SDK调用
        new Thread(() -> {
            try {
                // 构建多模态消息
                MultiModalConversation conv = new MultiModalConversation();
                List<Map<String, Object>> contentList = new ArrayList<>();
                //
                String urltest = "https://teenai-docs-1324829576.cos.ap-shanghai.myqcloud.com/public/audio/装修噪音.wav";
                // 添加音频URL
                if (request.getAudioUrl() != null && !request.getAudioUrl().isEmpty()) {
                    contentList.add(Collections.singletonMap("audio", request.getAudioUrl()));
                }
                
                // 构建提示词
                String prompt = buildAudioPrompt(request);
                // 添加文本提示词
                contentList.add(Collections.singletonMap("text", prompt));
                log.info("构建的提示词：{}", prompt);
                // 构建用户消息
                MultiModalMessage userMsg = MultiModalMessage.builder()
                        .role(Role.USER.getValue())
                        .content(contentList)
                        .build();

                // 构建调用参数
                MultiModalConversationParam param = MultiModalConversationParam.builder()
                        .apiKey(apiKey)
                        .model(audioModel)
                        .message(userMsg)
                        .enableSearch(false)
                        .incrementalOutput(true)
                        .build();

                // 调用SDK并处理流式响应
                conv.streamCall(param)
                        .doOnNext(result -> {
                            if (result != null && result.getOutput() != null
                                    && !result.getOutput().getChoices().isEmpty()) {
                                String rawContent = result.getOutput().getChoices().get(0).getMessage().getContent().toString();
                                if (rawContent != null && !rawContent.isEmpty()) {
                                    String cleanContent = rawContent
                                            .replaceAll("\\[|\\]", "")
                                            .replaceAll("\\{text=", "")
                                            .replaceAll("}", "");
                                    if (!cleanContent.isEmpty()) {
                                        aiResponseBuilder.append(cleanContent);
                                        sink.tryEmitNext(cleanContent);
                                    }
                                }
                            }
                        })
                        .doOnComplete(() -> {
                            long duration = System.currentTimeMillis() - startTime;
                            String aiContent = aiResponseBuilder.toString();
                            
                            if (aiContent.isEmpty()) {
                                aiContent = "未识别到有效内容";
                            }
                            
                            // 保存AI消息
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
                            log.info("AI 音频对话消息已保存，sessionId={}, 长度={}", request.getSessionId(), aiContent.length());
                            chatSessionService.incrementMessageCount(request.getSessionId());
                            sink.tryEmitComplete();
                        })
                        .doOnError(e -> {
                            long duration = System.currentTimeMillis() - startTime;
                            String errorMsg = "音频分析失败：" + e.getMessage();
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
                            log.error("AI音频消息分析失败并保存，sessionId={}, 错误={}", request.getSessionId(), e.getMessage(), e);
                            chatSessionService.incrementMessageCount(request.getSessionId());
                            sink.tryEmitNext(errorMsg);
                            sink.tryEmitComplete();
                        })
                        .subscribe();

            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                String errorMsg = "音频分析失败：" + e.getMessage();
                log.error("音频模型调用全局异常", e);
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

        return sink.asFlux().filter(Objects::nonNull);
    }

    /**
     * 构建音频对话提示词
     */
    private String buildAudioPrompt(ChatRequest request) {
        String userInfo = "";
        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            userInfo += "用户昵称 = " + request.getNickname();
        }
        if (request.getSex() != null) {
            userInfo += (userInfo.isEmpty() ? "" : ", ") + "用户性别 = " + (request.getSex() == 1 ? "男" : "女");
        }
        
        return """
           你是一名专业的青少年情感陪伴专家，请分析音频内容，并给予相应的情感支持和建议。
           %s
           当前日期：%s
           """.formatted(
               userInfo.isEmpty() ? "" : userInfo + "\n",
               getCurrentDate()
           );
    }
    
    /**
     * 获取当前日期
     */
    private String getCurrentDate() {
        return java.time.LocalDate.now().toString();
    }

    /**
     * 估算token数量
     */
    private int estimateTokens(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.length() / 3;
    }
}
