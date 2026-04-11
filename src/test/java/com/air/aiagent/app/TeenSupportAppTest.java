package com.air.aiagent.app;

import cn.hutool.core.lang.UUID;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class TeenSupportAppTest {

    @Autowired
    private TeenSupportApp teenSupportApp;

    @BeforeEach
    void setUp() {
        assertNotNull(teenSupportApp, "teenSupportApp 注入失败，请检查 TeenSupportApp 是否被 Spring 扫描为 Bean");
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        // 测试地图 MCP
        String message = "帮我搜索桃花的图片";
        String answer =  teenSupportApp.doChatWithMCP(message, chatId);
        System.out.println("地图 MCP 回答: " + answer);
        Assertions.assertNotNull(answer);
    }

}