package com.aiagenttest.agent;

import com.aiagenttest.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * AI 超级智能体
 */
@Component
public class AiManus extends ToolCallAgent {

    public AiManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("yuManus");
        String SYSTEM_PROMPT =
                "You are AiManus, an all-capable AI assistant, aimed at solving any task presented by the user.\n" +
                        "You have various tools at your disposal that you can call upon to efficiently complete complex requests.";
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT =
                "Based on user needs, proactively select the most appropriate tool or combination of tools.\n" +
                        "For complex tasks, you can break down the problem and use different tools step by step to solve it.\n" +
                        "After using each tool, clearly explain the execution results and suggest the next steps.\n" +
                        "If you want to stop the interaction at any point, use the `terminate` tool/function call.";
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(10);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
}
