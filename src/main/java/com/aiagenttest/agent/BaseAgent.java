package com.aiagenttest.agent;

import cn.hutool.core.util.StrUtil;

import com.aiagenttest.agent.model.AgentState;
import com.aiagenttest.model.ChatMessage;
import com.aiagenttest.repository.ChatMessageRepository;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象基础代理类，用于管理代理状态和执行流程。
 * <p>
 * 提供状态转换、内存管理和基于步骤的执行循环的基础功能。
 * 子类必须实现step方法。
 */
@Data
@Slf4j
public abstract class BaseAgent {

    // 核心属性
    private String name;

    // 提示词
    private String systemPrompt;
    private String nextStepPrompt;

    // 代理状态
    private AgentState state = AgentState.IDLE;

    // 执行步骤控制
    private int currentStep = 0;
    private int maxSteps = 10;

    // LLM 大模型
    private ChatClient chatClient;

    // Memory 记忆（需要自主维护会话上下文）
    private List<Message> messageList = new ArrayList<>();

    // 数据库聊天记忆存储
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // RAG云知识库
    @Resource
    private Advisor loveAppRagCloudAdvisor;

    // 打字机效果配置
    private long typewriterDelay = 50; // 每个字符的延迟（毫秒）
    private boolean enableTypewriter = true;

    /**
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {
        // 1、基础校验
        if (this.state != AgentState.IDLE) {
            throw new RuntimeException("Cannot run agent from state: " + this.state);
        }
        if (StrUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");
        }
        // 2、执行，更改状态
        this.state = AgentState.RUNNING;
        // 记录消息上下文
        messageList.add(new UserMessage(userPrompt));
        // 保存结果列表
        List<String> results = new ArrayList<>();
        try {
            // 执行循环
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                int stepNumber = i + 1;
                currentStep = stepNumber;
                log.info("Executing step {}/{}", stepNumber, maxSteps);
                // 单步执行
                String stepResult = step();
                String result = "Step " + stepNumber + ": " + stepResult;
                results.add(result);
            }
            // 检查是否超出步骤限制
            if (currentStep >= maxSteps) {
                state = AgentState.FINISHED;
                results.add("Terminated: Reached max steps (" + maxSteps + ")");
            }
            return String.join("\n", results);
        } catch (Exception e) {
            state = AgentState.ERROR;
            log.error("error executing agent", e);
            return "执行错误" + e.getMessage();
        } finally {
            // 3、清理资源
            this.cleanup();
        }
    }

    /**
     * 运行代理（流式输出）
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public SseEmitter runStreamWithTypewriter(String userPrompt, String conversationId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(300000L); // 5 分钟超时

        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 1、基础校验
                if (this.state != AgentState.IDLE) {
                    sendWithTypewriter(sseEmitter, "错误：无法从状态运行代理：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sendWithTypewriter(sseEmitter, "错误：不能使用空提示词运行代理");
                    sseEmitter.complete();
                    return;
                }

                // 2、加载历史对话记忆
                loadChatHistory(conversationId);

                // 3、构建用户消息（RAG会通过Advisor自动处理）
                String enhancedUserPrompt = userPrompt;

                // 5、执行，更改状态
                this.state = AgentState.RUNNING;

                // 记录用户消息到数据库和内存
                saveUserMessage(userPrompt, conversationId);
                messageList.add(new UserMessage(enhancedUserPrompt));

                // 保存结果列表
                List<String> results = new ArrayList<>();
                StringBuilder fullResponse = new StringBuilder();

                // 执行循环
                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{}", stepNumber, maxSteps);

                    // 单步执行
                    String stepResult = step();
                    String result = "Step " + stepNumber + ": " + stepResult;
                    results.add(result);
                    fullResponse.append(stepResult).append("\n");

                    // 使用打字机效果输出当前每一步的结果到 SSE
                    sendWithTypewriter(sseEmitter, result);
                }

                // 检查是否超出步骤限制
                if (currentStep >= maxSteps) {
                    state = AgentState.FINISHED;
                    String terminationMsg = "执行结束：达到最大步骤（" + maxSteps + "）";
                    results.add(terminationMsg);
                    sendWithTypewriter(sseEmitter, terminationMsg);
                }

                // 保存助手回复到数据库
                saveAssistantMessage(fullResponse.toString(), conversationId);

                // 正常完成
                sseEmitter.complete();

            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("error executing agent", e);
                try {
                    sendWithTypewriter(sseEmitter, "执行错误：" + e.getMessage());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                // 清理资源
                this.cleanup();
            }
        });

        // 设置超时回调
        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timeout");
        });

        // 设置完成回调
        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return sseEmitter;
    }

    /**
     * 打字机效果发送消息
     */
    private void sendWithTypewriter(SseEmitter sseEmitter, String message) throws IOException {
        if (!enableTypewriter) {
            sseEmitter.send(message);
            return;
        }

        // 逐字符发送，实现打字机效果
        for (char c : message.toCharArray()) {
            try {
                sseEmitter.send(String.valueOf(c));
                Thread.sleep(typewriterDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // 发送换行符
        sseEmitter.send("\n");
    }

    /**
     * 加载聊天历史记忆
     */
    private void loadChatHistory(String conversationId) {
        if (StrUtil.isBlank(conversationId)) {
            return;
        }

        try {
            // 从数据库加载历史消息
            List<ChatMessage> historyMessages = chatMessageRepository.lambdaQuery()
                    .eq(ChatMessage::getConversationId, conversationId)
                    .orderByAsc(ChatMessage::getCreateTime)
                    .list();

            // 转换为Message对象并添加到消息列表
            for (ChatMessage chatMessage : historyMessages) {
                Message message;
                if (chatMessage.getMessageType() == MessageType.USER) {
                    message = new UserMessage(chatMessage.getContent());
                } else if (chatMessage.getMessageType() == MessageType.ASSISTANT) {
                    message = new AssistantMessage(chatMessage.getContent());
                } else {
                    continue; // 跳过其他类型的消息
                }
                messageList.add(message);
            }

            log.info("Loaded {} messages from conversation: {}", historyMessages.size(), conversationId);
        } catch (Exception e) {
            log.error("Failed to load chat history for conversation: " + conversationId, e);
        }
    }



    /**
     * 保存用户消息到数据库
     */
    private void saveUserMessage(String content, String conversationId) {
        try {
            ChatMessage chatMessage = ChatMessage.builder()
                    .conversationId(conversationId)
                    .messageType(MessageType.USER)
                    .content(content)
                    .metadata(new java.util.HashMap<>()) // 设置空的metadata
                    .build();
            chatMessageRepository.save(chatMessage);
        } catch (Exception e) {
            log.error("Failed to save user message", e);
        }
    }

    /**
     * 保存助手消息到数据库
     */
    private void saveAssistantMessage(String content, String conversationId) {
        try {
            ChatMessage chatMessage = ChatMessage.builder()
                    .conversationId(conversationId)
                    .messageType(MessageType.ASSISTANT)
                    .content(content)
                    .metadata(new java.util.HashMap<>()) // 设置空的metadata
                    .build();
            chatMessageRepository.save(chatMessage);
        } catch (Exception e) {
            log.error("Failed to save assistant message", e);
        }
    }

    /**
     * 定义单个步骤
     *
     * @return
     */
    public abstract String step();

    /**
     * 清理资源
     */
    protected void cleanup() {
        // 子类可以重写此方法来清理资源
    }

    /**
     * 设置打字机效果延迟
     */
    public void setTypewriterDelay(long delayMs) {
        this.typewriterDelay = delayMs;
    }

    /**
     * 启用/禁用打字机效果
     */
    public void setEnableTypewriter(boolean enable) {
        this.enableTypewriter = enable;
    }
}
