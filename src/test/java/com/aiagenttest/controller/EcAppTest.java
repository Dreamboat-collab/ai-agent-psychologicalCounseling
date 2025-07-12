package com.aiagenttest.controller;

import com.aiagenttest.app.EcApp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
public class EcAppTest {
    @Autowired
    EcApp ecApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是taotao";
        String answer = ecApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第二轮
        message = "如何缓解学业压力";
        answer = ecApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }
}
