package com.air.aiagent.app;

import cn.hutool.core.lang.UUID;
import com.air.aiagent.advisor.MyLoggerAdvisor;
import com.air.aiagent.constant.SystemConstants;
import com.air.aiagent.domain.dto.ChatRequest;
import com.air.aiagent.domain.entity.ChatMessage;
import com.air.aiagent.domain.entity.KnowledgeBase;
import com.air.aiagent.domain.entity.MessageMetadata;
import com.air.aiagent.domain.entity.MessageType;
import com.air.aiagent.rag.PgVectorStoreConfig;
import com.air.aiagent.service.KnowledgeBaseService;
import com.air.aiagent.service.impl.ChatMessageService;
import com.air.aiagent.service.impl.ChatSessionService;
import com.air.aiagent.utils.AudioRecognizer;
import com.air.aiagent.utils.ImageRecognizer;
import com.air.aiagent.utils.IntentRecognizer;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.air.aiagent.constant.SystemConstants.getCurrentDate;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Slf4j
@Component
public class TeenSupportApp {

    private final ChatClient chatClient;
    private final ChatClient gameClient;
    private final ChatMemory chatMemory;
    private final ChatMemory gameMemory;
    private final ChatMemory emoMemory;
    private final ChatClient emoClient;
    // 新增：联网搜索开关（可配置化）
    @Value("${spring.ai.dashscope.chat.options.enable-search}")
    private boolean enableSearch;
    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ChatMessageService chatMessageService;
    /**
     * 图像识别器
     */
    @Resource
    private ImageRecognizer imageRecognizer;

    /**
     * 音频识别器
     */
    @Resource
    private AudioRecognizer audioRecognizer;

    /**
     * 意图识别器
     */
    @Resource
    private IntentRecognizer intentRecognizer;
    @Resource
    private VectorStore teenSupportVectorStore;

    @Resource
    private PgVectorStoreConfig pgVectorStoreConfig;

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    @Resource
    @Qualifier("pgJdbcTemplate")
    private JdbcTemplate pgJdbcTemplate;

    @Resource
    private EmbeddingModel dashscopeEmbeddingModel;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    public TeenSupportApp(ChatModel dashscopeChatModel) {
        chatMemory = new InMemoryChatMemory();
        gameMemory = new InMemoryChatMemory();
        emoMemory = new InMemoryChatMemory();

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SystemConstants.CHAT_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor()
                )
                .build();

        gameClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SystemConstants.GAME_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(gameMemory),
                        new MyLoggerAdvisor()
                )
                .build();

        emoClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SystemConstants.EMOTION_DETECTION)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(emoMemory),
                        new MyLoggerAdvisor()
                ).build();
    }

    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();

        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    private int estimateTokens(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.length() / 3;
    }

    private void saveUserMessage(ChatRequest request) {
        String userMessageId = UUID.randomUUID().toString();
        ChatMessage userMessage = ChatMessage.builder()
                .id(userMessageId)
                .chatId(request.getChatId())
                .sessionId(request.getSessionId())
                .messageType(MessageType.TEXT)
                .content(request.getMessage())
                .isAiResponse(false)
                .build();
        chatMessageService.save(userMessage);
        log.info("用户消息已保存，sessionId={}, 内容长度={}", request.getSessionId(), request.getMessage().length());
    }
    /**
     * 智能对话入口
     */
    public Flux<String> smartChat(ChatRequest request ,MessageType type) {

        if (type == MessageType.TEXT) {
            log.info("文字对话模式");
            return doChatWithRagAndTools(request);
        } else if (type == MessageType.IMAGE) {
            log.info("图像理解模式");
            return imageRecognizer.recognizeScene(request);
        } else if (type == MessageType.AUDIO) {
            log.info("音频理解模式");
            return audioRecognizer.recognizeAudio(request);
        } else {
            log.warn("未知的消息类型: {}", type);
            return Flux.just("不支持的消息类型");
        }
    }
    public Flux<String> doChatWithRagAndTools(ChatRequest request) {
        saveUserMessage(request);
        // 1. 意图识别：判断是否需要使用 RAG 知识库
        boolean needRag = intentRecognizer.needKnowledgeBase(request.getMessage());

        List<ChatMessage> historyMessages = chatMessageService
                .findHistoryExcludingLatest(request.getSessionId(), 50, 1);
        log.info("获取用户，id={}，历史上下文，数量={}", request.getChatId(), historyMessages.size());

        if (historyMessages.isEmpty()) {
            boolean success = chatSessionService.updateSessionName(request.getSessionId(),
                    request.getChatId(), request.getMessage());
            if (success) {
                log.info("会话名称已更新为：{}", request.getMessage());
            } else {
                log.warn("会话名称更新失败，sessionId: {}", request.getSessionId());
            }
        }

        String finalMessage;
        if (needRag) {
            // 获取所有激活的知识库
            List<KnowledgeBase> activeKnowledgeBases = knowledgeBaseService.getActiveKnowledgeBaseList();
            
            if (activeKnowledgeBases.isEmpty()) {
                log.info("没有激活的知识库，不使用RAG，直接对话");
                finalMessage = "用户问题：" + request.getMessage();
            } else {
                log.info("启用 RAG 知识库检索，共 {} 个激活的知识库", activeKnowledgeBases.size());
                
                // 获取所有激活知识库的表名
                List<String> tableNames = activeKnowledgeBases.stream()
                        .map(KnowledgeBase::getTableName)
                        .toList();
                
                // 获取对应的VectorStore列表
                List<VectorStore> vectorStores = pgVectorStoreConfig.getVectorStoresByTableNames(
                        pgJdbcTemplate, dashscopeEmbeddingModel, tableNames);
                
                // 从所有知识库中检索文档
                List<Document> allRelevantDocs = new ArrayList<>();
                for (VectorStore vectorStore : vectorStores) {
                    try {
                        List<Document> docs = vectorStore.similaritySearch(
                            SearchRequest.builder()
                                .query(request.getMessage())
                                .topK(10)
                                .similarityThreshold(0.54)
                                .build()
                        );
                        allRelevantDocs.addAll(docs);
                        log.info("从知识库检索到 {} 条文档", docs.size());
                    } catch (Exception e) {
                        log.error("从知识库检索文档失败", e);
                    }
                }
                
                log.info("从所有知识库共检索到 {} 条相关文档", allRelevantDocs.size());

                // 文档去重，保留最多5条
                List<Document> uniqueDocs = new ArrayList<>();
                Set<String> seenContents = new HashSet<>();

                for (Document doc : allRelevantDocs) {
                    String content = doc.getText();
                    if (!seenContents.contains(content)) {
                        seenContents.add(content);
                        uniqueDocs.add(doc);
                        if (uniqueDocs.size() >= 5) {
                            break;
                        }
                    }
                }

                StringBuilder contextBuilder = new StringBuilder();
                if (!uniqueDocs.isEmpty()) {
                    contextBuilder.append("以下是相关的参考资料（仅作参考，请优先基于对话历史回答）：\n\n");
                    for (int i = 0; i < uniqueDocs.size(); i++) {
                        Document doc = uniqueDocs.get(i);
                        contextBuilder.append("资料 ").append(i + 1).append(":\n");
                        contextBuilder.append(doc.getText()).append("\n\n");
                    }
                    contextBuilder.append("请根据以上参考资料回答用户的问题。如果参考资料与用户相关的话题没有关联度，则不参考资料，直接回答\n\n");
                }

                finalMessage = contextBuilder.toString() + "用户问题：" + request.getMessage();
                log.info("RAG 检索到 {} 条相关文档，去重后剩余 {} 条", allRelevantDocs.size(), uniqueDocs.size());
            }
        } else {
            log.info("不启用 RAG 知识库，直接使用用户消息对话");
            finalMessage = "用户问题：" + request.getMessage();
        }

        log.info("用户 id={}，完整提示词构建完成，长度={}", request.getChatId(), finalMessage.length());

        StringBuilder aiResponseBuilder = new StringBuilder();
        String aiMessageId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        // ========== 新增：构建联网搜索配置 ==========
        log.info("联网搜索是否开启：" + enableSearch);
        DashScopeChatOptions chatOptions = DashScopeChatOptions.builder()
                .withEnableSearch(enableSearch) // 核心：开启联网搜索
                .build();

        /**
         * 修复说明：
         *
         * - 之前的代码没有设置对话记忆的会话 ID 参数
         * - 现在使用 sessionId 作为对话记忆的 key（而不是 chatId）
         * - 设置每次获取最近 10 条历史记录
         */
        var promptBuilder = chatClient.prompt()
                .user("userId = " + request.getChatId() + ", sessionId = " + request.getSessionId() + 
                      (request.getNickname() != null && !request.getNickname().isEmpty() ? 
                       ", 用户昵称 = " + request.getNickname() : "") + "," + finalMessage + "\n当前日期：%s".formatted(getCurrentDate()))
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, request.getSessionId())
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 50))
                .tools(allTools)
                .tools(toolCallbackProvider);
//                .options(chatOptions); // 关键：传入联网搜索配置

        return promptBuilder
                .stream()
                .content()
                .doOnNext(chunk -> aiResponseBuilder.append(chunk))
                .doOnComplete(() -> {
                    long duration = System.currentTimeMillis() - startTime;
                    String aiContent = aiResponseBuilder.toString();
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
                })
                .doOnError(error -> {
                    log.error("AI 流式输出异常，sessionId={}", request.getSessionId(), error);
                    if (aiResponseBuilder.length() > 0) {
                        String errorContent = aiResponseBuilder.toString() + "\n[流式输出中断]";
                        ChatMessage errorMessage = ChatMessage.builder()
                                .id(aiMessageId)
                                .chatId(request.getChatId())
                                .sessionId(request.getSessionId())
                                .content(errorContent)
                                .messageType(MessageType.TEXT)
                                .isAiResponse(true)
                                .build();
                        chatMessageService.save(errorMessage);
                        log.warn("AI 错误消息已保存，sessionId={}, 长度={}", request.getSessionId(), errorContent.length());
                        chatSessionService.incrementMessageCount(request.getSessionId());
                    }
                });
    }

    public String doChatWithMCP(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .tools(toolCallbackProvider)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    public record SupportReport(String title, List<String> suggestions) {
    }

    public SupportReport doChatWithReport(String message, String chatId) {
        SupportReport supportReport = chatClient
                .prompt()
                .system(SystemConstants.CHAT_SYSTEM_PROMPT + "每次对话后都要生成成长陪伴结果，标题为{用户名}的成长陪伴报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(SupportReport.class);
        log.info("supportReport: {}", supportReport);
        return supportReport;
    }

    public Flux<String> gameStreamChat(String message, String chatId) {
        return gameClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }

    public String doChatWithEmo(String message, String chatId) {
        ChatResponse chatResponse = emoClient
                .prompt()
                .user(message)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }
}
