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
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import com.air.aiagent.utils.SessionIdGenerator;
import java.util.List;

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
        return chatClient.prompt()
                .user("userId = "+chatId+","+message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
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
