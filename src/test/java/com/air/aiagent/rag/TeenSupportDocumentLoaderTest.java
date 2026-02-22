package com.air.aiagent.rag;
import com.air.aiagent.app.TeenSupportApp;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


import java.util.UUID;

@SpringBootTest
class TeenSupportDocumentLoaderTest {


    @Resource
    private TeenSupportApp teenSupportApp;

    @Resource
    TeenSupportDocumentLoader teenSupportDocumentLoader;


    @Test
    void loadMarkdown() {
        teenSupportDocumentLoader.loadMarkdown();
    }

    @Test
    void doChat() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是一名高中生";
        String answer = teenSupportApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        message = "我最近学习压力很大，不知道该怎么办";
        answer = teenSupportApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        message = "你刚才跟我说了什么？帮我回忆一下";
        answer = teenSupportApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我最近考试没考好，心情很低落";
        TeenSupportApp.SupportReport supportReport = teenSupportApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(supportReport);
    }

}
