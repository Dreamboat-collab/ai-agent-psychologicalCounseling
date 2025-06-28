package com.aiagenttest.app;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Component
@Slf4j
public class LoveApp {

    private final ChatClient chatClient;

    //定义健康报告
    public record HealthReport(String title, List<String> suggestions) {
    }

//    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
//            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
//            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
//            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

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

    public LoveApp(ChatModel dashscopeChatModel) {
        // 初始化基于内存的对话记忆
        ChatMemory chatMemory = new InMemoryChatMemory();

        //初始化ChatClient
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory) //存储对话记忆
                )
                .build();
    }

    //对话方法，调用chatClient
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

    public void Films() {
        // 定义一个记录类
        record ActorsFilms(String actor, List<String> movies) {}

        // 使用高级 ChatClient API
        ActorsFilms actorsFilms = chatClient.prompt()
                .user("Generate 5 movies for Tom Hanks.")
                .call()
                .entity(ActorsFilms.class);

        List<String> moviesList = actorsFilms.movies(); // 访问 movies 字段
        System.out.println(moviesList);
    }


    //心理健康报告
    public HealthReport testReport(String message, String chatId) {
        HealthReport report = chatClient.prompt()
                .system(SYSTEM_PROMPT + "每次对话后都需要生成报告结果，标题为{用户名}的健康报告，内容为建议列表")
                .user(message)
                .advisors(s -> s.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)  //s是AdvisorSpec 是一个对话记忆和上下文管理的配置器，它的核心作用是为当前对话设置记忆存储规则、上下文检索策略
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .entity(HealthReport.class);
        return report;
    }
}

