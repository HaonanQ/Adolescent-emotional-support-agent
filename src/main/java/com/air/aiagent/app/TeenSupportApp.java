package com.air.aiagent.app;
import com.air.aiagent.advisor.MyLoggerAdvisor;
import com.air.aiagent.constant.SystemConstants;
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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import com.air.aiagent.utils.SessionIdGenerator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * @author WyH524
 * @since 2025/7/24 上午10:48
 */
@Slf4j
@Component
public class TeenSupportApp {

    private final ChatClient chatClient;

    private final ChatClient gameClient;

    private final ChatMemory chatMemory;

    private final ChatMemory gameMemory;

    private final ChatMemory emoMemory;

    private final ChatClient emoClient;


    /**
     * 初始化 AI 客户端 ChatClient
     */
    public TeenSupportApp(ChatModel dashscopeChatModel) {
        chatMemory = new InMemoryChatMemory();
        gameMemory = new InMemoryChatMemory();
        emoMemory = new InMemoryChatMemory();

        /**
         * 初始化 ChatClient
         */
        chatClient=ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SystemConstants.CHAT_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory),
                        new MyLoggerAdvisor()
                )
                .build();

        /**
         * 初始化 GameClient
         */
        gameClient=ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SystemConstants.GAME_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(gameMemory),
                        new MyLoggerAdvisor()
                )
                .build();

        emoClient=ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SystemConstants.EMOTION_DETECTION)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(emoMemory),
                        new MyLoggerAdvisor()
                ).build();
    }


    /**
     * AI 基础对话（支持多轮对话记忆）
     */
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


    /**
     * AI 青少年情感知识库问答功能
     */
    @Resource
    private VectorStore teenSupportVectorStore;

    @Resource
    private Advisor teenSupportRagCloudAdvisor;

    @Resource
    private VectorStore pgVectorVectorStore;

    /**
     * AI 调用工具能力
     */
    @Resource
    private ToolCallback[] allTools;


    //RAG 知识库进行对话
    public Flux<String> doChatWithRagAndTools(String message, String chatId){
        // 1. 首先从向量数据库中检索相关文档
        List<Document> relevantDocs = pgVectorVectorStore.similaritySearch(
            SearchRequest.builder()
                .query(message)
                .topK(5)
                .build()
        );
        
        // 2. 去重处理，避免重复的文档
        List<Document> uniqueDocs = new ArrayList<>();
        Set<String> seenContents = new HashSet<>();
        
        for (Document doc : relevantDocs) {
            String content = doc.getText();
            if (!seenContents.contains(content)) {
                seenContents.add(content);
                uniqueDocs.add(doc);
                // 最多保留3条不同的文档
                if (uniqueDocs.size() >= 3) {
                    break;
                }
            }
        }
        
        // 3. 构建上下文信息
        StringBuilder contextBuilder = new StringBuilder();
        if (!uniqueDocs.isEmpty()) {
            contextBuilder.append("以下是相关的参考资料：\n\n");
            for (int i = 0; i < uniqueDocs.size(); i++) {
                Document doc = uniqueDocs.get(i);
                contextBuilder.append("资料 ").append(i + 1).append(":\n");
                contextBuilder.append(doc.getText()).append("\n\n");
            }
            contextBuilder.append("请根据以上参考资料回答用户的问题。\n\n");
        }
        
        // 4. 将上下文和用户消息组合
        String finalMessage = contextBuilder.toString() + "用户问题：" + message;
        
        log.info("RAG 检索到 {} 条相关文档，去重后剩余 {} 条", relevantDocs.size(), uniqueDocs.size());
        if (!uniqueDocs.isEmpty()) {
            log.info("上下文内容：{}", contextBuilder.toString());
        }
        
        return chatClient.prompt()
                .user(finalMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .tools(allTools)
                .stream()
                .content();
    }


    /**
     * AI调用MCP服务
     */
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

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


    /**
     * 成长陪伴报告
     */
    public record SupportReport(String title, List<String> suggestions){

    }

    /**
     * 结构化输出，成长陪伴报告功能
     */
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


    /**
     * 心情小游戏
     */
    public Flux<String> gameStreamChat(String message, String chatId){
        return gameClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }


    /**
     * 情绪返回
     */
    public String doChatWithEmo(String message, String chatId) {
        ChatResponse chatResponse = emoClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }
}
