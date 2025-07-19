package com.aiagenttest.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.aiagenttest.agent.model.AgentState;
import com.aiagenttest.model.ChatMessage;
import com.aiagenttest.repository.ChatMessageRepository;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 增强版工具调用代理类，集成了数据库聊天记忆、阿里RAG和打字机回复效果
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存工具调用信息的响应结果（要调用那些工具）
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    // ========== 增强功能字段 ==========
    
    // 数据库聊天记忆存储
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    // RAG云知识库
    @Resource
    private Advisor loveAppRagCloudAdvisor;

    // 打字机效果配置
    private long typewriterDelay = 50; // 每个字符的延迟（毫秒）
    private boolean enableTypewriter = true;
    
    // 会话ID
    private String conversationId;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = DashScopeChatOptions.builder()
                .withProxyToolCalls(true)
                .build();
    }

    /**
     * 处理当前状态并决定下一步行动（增强版，集成RAG）
     *
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        // 1、校验提示词，拼接用户提示词
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        
        // 2、加载历史对话记忆（如果有会话ID）
        loadChatHistory();
        
        // 3、调用 AI 大模型，集成RAG和工具调用
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .advisors(loveAppRagCloudAdvisor) // 集成阿里RAG
                    .tools(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应，用于等下 Act
            this.toolCallChatResponse = chatResponse;
            
            // 4、解析工具调用结果，获取要调用的工具
            // 助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 输出提示信息
            String result = assistantMessage.getText();
            log.info(getName() + "的思考（RAG增强）：" + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            
            // 5、保存用户消息到数据库（如果是新的用户输入）
            if (StrUtil.isNotBlank(getNextStepPrompt())) {
                saveUserMessage(getNextStepPrompt());
            }
            
            // 如果不需要调用工具，返回 false
            if (toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                getMessageList().add(assistantMessage);
                // 保存助手回复到数据库（保存完整的响应内容）
                saveAssistantMessage(result);
                log.info("AI响应已保存，内容长度: {}", result != null ? result.length() : 0);
                return false;
            } else {
                // 需要调用工具时，无需记录助手消息，因为调用工具时会自动记录
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考过程遇到了问题：" + e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果
     *
     * @return 执行结果
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具需要调用";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        // 第一个参数表示消息上下文，第二个参数表示AI大模型返回的响应
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // 判断是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            // 任务结束，更改状态
            setState(AgentState.FINISHED);
        }
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 返回的结果：" + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }

    // ========== 增强功能方法 ==========

    /**
     * 加载聊天历史记忆
     */
    private void loadChatHistory() {
        if (StrUtil.isBlank(conversationId) || chatMessageRepository == null) {
            return;
        }

        try {
            // 从数据库加载历史消息
            List<ChatMessage> historyMessages = chatMessageRepository.lambdaQuery()
                    .eq(ChatMessage::getConversationId, conversationId)
                    .orderByAsc(ChatMessage::getCreateTime)
                    .list();

            // 只有在消息列表为空时才加载历史记录，避免重复加载
            if (getMessageList().isEmpty() && !historyMessages.isEmpty()) {
                for (ChatMessage chatMessage : historyMessages) {
                    Message message;
                    if (chatMessage.getMessageType() == MessageType.USER) {
                        message = new UserMessage(chatMessage.getContent());
                    } else if (chatMessage.getMessageType() == MessageType.ASSISTANT) {
                        message = new AssistantMessage(chatMessage.getContent());
                    } else {
                        continue; // 跳过其他类型的消息
                    }
                    getMessageList().add(message);
                }
            }
            
            log.info("Loaded {} messages from conversation: {}", historyMessages.size(), conversationId);
        } catch (Exception e) {
            log.error("Failed to load chat history for conversation: " + conversationId, e);
        }
    }

    /**
     * 保存用户消息到数据库
     */
    private void saveUserMessage(String content) {
        if (StrUtil.isBlank(conversationId) || chatMessageRepository == null) {
            return;
        }
        
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
    private void saveAssistantMessage(String content) {
        if (StrUtil.isBlank(conversationId) || chatMessageRepository == null) {
            return;
        }
        
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
     * 增强版运行方法，带打字机效果的流式输出
     */
    public SseEmitter runStreamWithTypewriter(String userPrompt) {
        return runStreamWithTypewriter(userPrompt, this.conversationId);
    }

    /**
     * 增强版运行方法，带打字机效果的流式输出
     */
    public SseEmitter runStreamWithTypewriter(String userPrompt, String conversationId) {
        // 设置会话ID
        if (StrUtil.isNotBlank(conversationId)) {
            this.conversationId = conversationId;
        }

        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(300000L); // 5 分钟超时

        // 使用线程异步处理，避免阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 1、重置Agent状态为IDLE，准备新的对话
                this.setState(AgentState.IDLE);
                
                // 2、基础校验
                if (StrUtil.isBlank(userPrompt)) {
                    try {
                        sendWithTypewriter(sseEmitter, "错误：不能使用空提示词运行代理");
                    } catch (IOException e) {
                        log.warn("Client disconnected during error message");
                    }
                    sseEmitter.complete();
                    return;
                }

                // 2、加载历史对话记忆
                loadChatHistory();

                // 3、执行，更改状态
                this.setState(AgentState.RUNNING);
                
                // 记录用户消息到数据库和内存
                saveUserMessage(userPrompt);
                getMessageList().add(new UserMessage(userPrompt));

                // 记录是否有最终回复
                boolean hasFinalResponse = false;

                // 执行循环
                for (int i = 0; i < getMaxSteps() && getState() != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    setCurrentStep(stepNumber);
                    log.info("Executing step {}/{}", stepNumber, getMaxSteps());
                    
                    // 执行思考
                    boolean needsAction = think();
                    String thinkResult = "思考阶段完成";
                    
                    if (needsAction) {
                        // 执行行动
                        String actionResult = act();
                        String stepResult = "第" + stepNumber + "步: " + thinkResult + " -> " + actionResult;
                        
                        // 发送工具调用思考过程（使用特殊事件类型）,这部分内容不直接显示在ai回复内容中，做特殊处理
                        try {
                            sseEmitter.send(SseEmitter.event()
                                    .name("tool_thinking")
                                    .data(stepResult));
                        } catch (IOException e) {
                            log.warn("Client disconnected during step output, terminating early");
                            return; // 客户端断开连接，提前结束
                        }
                    } else {
                        // 没有工具调用，获取AI的实际响应内容
                        String aiResponse = getLastAssistantResponse();
                        if (StrUtil.isNotBlank(aiResponse)) {
                            try {
                                // 发送正常AI回复（使用打字机效果）
                                sendWithTypewriter(sseEmitter, aiResponse);
                                // 保存AI回复到数据库
                                saveAssistantMessage(aiResponse);
                                hasFinalResponse = true;
                            } catch (IOException e) {
                                log.warn("Client disconnected during AI response output, terminating early");
                                return;
                            }
                        } else {
                            String finalResult = "任务完成";
                            try {
                                sendWithTypewriter(sseEmitter, finalResult);
                                // 保存最终结果到数据库
                                saveAssistantMessage(finalResult);
                                hasFinalResponse = true;
                            } catch (IOException e) {
                                log.warn("Client disconnected during final result output, terminating early");
                                return;
                            }
                        }
                        break;
                    }
                }

                // 检查是否超出步骤限制
                if (getCurrentStep() >= getMaxSteps()) {
                    setState(AgentState.FINISHED);
                    String terminationMsg = "执行结束：达到最大步骤（" + getMaxSteps() + "）";
                    try {
                        sendWithTypewriter(sseEmitter, terminationMsg);
                    } catch (IOException e) {
                        log.warn("Client disconnected during termination message, completing silently");
                    }
                }

                // 检查是否达到最大步骤且还没有最终回复
                if (getCurrentStep() >= getMaxSteps() && !hasFinalResponse) {
                    setState(AgentState.FINISHED);
                    // 强制生成最终回复
                    String finalSummary = generateFinalSummary();
                    if (StrUtil.isNotBlank(finalSummary)) {
                        try {
                            sendWithTypewriter(sseEmitter, finalSummary);
                        } catch (IOException e) {
                            log.warn("Client disconnected during final summary, terminating early");
                            return;
                        }
                        // 保存最终回复到数据库
                        saveAssistantMessage(finalSummary);
                        hasFinalResponse = true;
                    }
                }

                // 发送结束信号给前端
                try {
                    sseEmitter.send(SseEmitter.event().name("close").data("DONE"));
                } catch (IOException e) {
                    log.warn("Failed to send close event: {}", e.getMessage());
                }
                
                // 正常完成
                sseEmitter.complete();
                
            } catch (Exception e) {
                setState(AgentState.ERROR);
                log.error("error executing enhanced agent", e);
                try {
                    sendWithTypewriter(sseEmitter, "执行错误：" + e.getMessage());
                    sseEmitter.complete();
                } catch (IOException ex) {
                    log.warn("Client disconnected during error message transmission");
                    sseEmitter.completeWithError(ex);
                }
            } finally {
                // 清理资源
                cleanup();
            }
        });

        // 设置超时回调
        sseEmitter.onTimeout(() -> {
            this.setState(AgentState.ERROR);
            cleanup();
            log.warn("SSE connection timeout");
        });
        
        // 设置完成回调
        sseEmitter.onCompletion(() -> {
            if (this.getState() == AgentState.RUNNING) {
                this.setState(AgentState.FINISHED);
            }
            cleanup();
            log.info("SSE connection completed");
        });
        
        return sseEmitter;
    }

    /**
     * 打字机效果发送消息
     */
    private void sendWithTypewriter(SseEmitter sseEmitter, String message) throws IOException {
        if (!enableTypewriter) {
            try {
                sseEmitter.send(message);
            } catch (IOException e) {
                log.warn("Failed to send message to SSE client: {}", e.getMessage());
                throw e;
            }
            return;
        }

        // 逐字符发送，实现打字机效果
        for (char c : message.toCharArray()) {
            try {
                sseEmitter.send(String.valueOf(c));
                Thread.sleep(typewriterDelay);  // 延时50ms后进行发送，类似打字机的效果
            } catch (IOException e) {
                log.warn("SSE connection closed by client, stopping typewriter effect");
                return; // 连接关闭，停止发送
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Typewriter thread interrupted");
                return;
            }
        }
        
        // 发送换行符
        try {
            sseEmitter.send("\n");
        } catch (IOException e) {
            log.warn("Failed to send newline to SSE client: {}", e.getMessage());
            // 不重新抛出异常，因为消息主体已经发送完成
        }
    }

    /**
     * 增强的清理方法
     */
    protected void cleanup() {
        this.toolCallChatResponse = null;
        // 重置Agent状态为IDLE，准备下次对话
        this.setState(AgentState.IDLE);
        log.info("Enhanced ToolCallAgent cleanup completed");
    }

    /**
     * 设置增强的系统提示词，包含RAG和工具使用指导
     */
    public void setEnhancedSystemPrompt() {
        String enhancedPrompt = """
            你是一个智能助手，具有以下能力：
            1. 可以访问心理健康知识库来获取相关信息
            2. 可以调用各种工具来完成任务
            3. 会记住完整的对话历史
            
            工具使用规则：
            - 首先利用知识库中的信息进行思考
            - 如果知识库无法回答用户问题，再考虑使用工具
            - 使用工具时要有明确的目标，避免重复调用相同工具
            - 如果一个工具调用没有得到满意结果，应该尝试不同的搜索词或其他工具
            - 获得工具结果后，必须基于结果给出最终回答，不要继续重复调用
            - 最多进行3次工具调用，然后必须给出最终回答
            
            回答格式：
            - 如果使用了工具，请基于工具结果给出具体、有用的回答
            - 避免仅仅描述调用了什么工具，要给出实际的解决方案
            """;
        setSystemPrompt(enhancedPrompt);
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

    /**
     * 获取最后一条助手响应的内容
     */
    String getLastAssistantResponse() {
        // 从消息列表中获取最后一条助手消息
        for (int i = getMessageList().size() - 1; i >= 0; i--) {
            Message message = getMessageList().get(i);
            if (message instanceof AssistantMessage) {
                return ((AssistantMessage) message).getText();
            }
        }
        return null;
    }

    /**
     * 当达到最大步骤时生成最终回复摘要
     */
    private String generateFinalSummary() {
        try {
            // 创建一个简单的提示词要求AI总结
            String summaryPrompt = "请根据前面的工具调用结果，为用户提供一个简洁明了的最终回答。请直接给出结论和建议，不要重复工具调用的详细过程。";
            
            UserMessage summaryUserMessage = new UserMessage(summaryPrompt);
            getMessageList().add(summaryUserMessage);
            
            List<Message> messageList = getMessageList();
            Prompt prompt = new Prompt(messageList, this.chatOptions);
            
            ChatResponse summaryResponse = getChatClient().prompt(prompt)
                    .system("你是一个有用的助手。请基于前面的工具调用结果，为用户提供简洁明了的最终回答。")
                    .call()
                    .chatResponse();
            
            AssistantMessage summaryMessage = summaryResponse.getResult().getOutput();
            getMessageList().add(summaryMessage);
            
            return summaryMessage.getText();
        } catch (Exception e) {
            log.error("Failed to generate final summary", e);
            return "抱歉，基于搜索结果，我找到了一些心理咨询机构的信息，但无法确定具体位置。建议您使用地图应用搜索附近的心理咨询服务。";
        }
    }
}
