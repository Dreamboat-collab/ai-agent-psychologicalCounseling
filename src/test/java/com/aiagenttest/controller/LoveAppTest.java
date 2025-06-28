package com.aiagenttest.controller;

import com.aiagenttest.app.LoveApp;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是taotao";
        String answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第二轮
        message = "我想让另一半（沛沛子）更爱我";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我的另一半叫什么来着？刚跟你说过，帮我回忆一下";
        answer = loveApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void testFilms() {
        loveApp.Films();
    }

    @Test
    void testReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是taotao，我感觉工作压力大，但我不知道该怎么做";
        LoveApp.HealthReport Report = loveApp.testReport(message, chatId);
        System.out.println(Report);
        Assertions.assertNotNull(Report);
    }
}

