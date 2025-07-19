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

    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("周末想带女朋友去上海约会，推荐几个适合情侣的小众打卡地？");

        // 测试网页抓取：恋爱案例分析
        testMessage("最近和对象吵架了，看看编程导航网站（codefather.cn）的其他情侣是怎么解决矛盾的？");

        // 测试资源下载：图片下载
        testMessage("直接下载一张适合做手机壁纸的星空情侣图片为文件");

        // 测试终端操作：执行代码
        testMessage("执行 Python3 脚本来生成数据分析报告");

        // 测试文件操作：保存用户档案
        testMessage("保存我的恋爱档案为文件");

        // 测试 PDF 生成
        testMessage("生成一份‘七夕约会计划’PDF，包含餐厅预订、活动流程和礼物清单");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = loveApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }


    // 测试RAG效果
    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "睡眠不足会影响心理健康吗？";
        String answer =  loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    //测试地图MCP
//    @Test
//    void doChatWithMcp() {
//        String chatId = UUID.randomUUID().toString();
//        // 测试地图 MCP
//        String message = "我住在龙华红山，帮我找到附近5km的心理咨询室";
//        String answer =  loveApp.doChatWithMcp(message, chatId);
//    }

    //测试调用MCP服务端工具
    @Test
    void doChatWithMcp() {
        // 测试图片搜索 MCP
        String chatId = UUID.randomUUID().toString();
        String message = "帮我搜索一些哄另一半开心的图片";
        String answer =  loveApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }
}

