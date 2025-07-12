package com.aiagenttest.app;

import com.aiagenttest.chatmemory.DatabaseChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
public class EcApp {
    private final ChatClient chatClient;

    //心理健康prompt
    private static final String SYSTEM_PROMPT = "扮演专业心智健康顾问，开场表明身份并声明提供安全倾诉空间。引导用户根据所处成长阶段分类探讨："
            + "青少年期（12-18岁）关注学业压力、社交认同感与家庭关系；"
            + "成年期（19-45岁）聚焦职业倦怠、亲密关系与自我价值定位；"
            + "中老年期（46岁+）探讨空巢适应、衰老焦虑及人生意义重构。"
            + "通过「现象描述+情绪反应+持续时长」三要素收集信息，结合认知行为疗法原则给出可操作建议。"
            + "重要原则："
            + "1. 严格保密虚拟案例数据；"
            + "2. 危机情况（如自伤倾向）立即提供专业援助渠道；"
            + "3. 避免诊断结论，强调‘非替代医疗建议’。";

    public EcApp(ChatModel dashscopeChatModel, DatabaseChatMemory databaseChatMemory) {
        String memoryPath = System.getProperty("user.dir") + "/tmp/chat-memory";
        // ChatMemory chatMemory = new InMemoryChatMemory();
        // ChatMemory chatMemory = new FileBasedChatMemory(memoryPath);
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(databaseChatMemory)
                )
                .build();
    }

    public String doChat(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId) //指定对话id
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10)) //指定历史消息的数量，10条，添加到当前会话的提示词中
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        System.out.println(content);
        return content;
    }
}
