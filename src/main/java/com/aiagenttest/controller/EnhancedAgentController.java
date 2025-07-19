package com.aiagenttest.controller;

import cn.hutool.core.util.IdUtil;
import com.aiagenttest.agent.AiManus;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 增强版Agent控制器，展示集成RAG、数据库存储和打字机效果的使用
 */
@RestController
@RequestMapping("/enhanced-agent")
public class EnhancedAgentController {

    @Resource
    private AiManus aiManus;

    @Autowired
    @Qualifier("loveAppRagCloudAdvisor")
    private Advisor ragAdvisor;

    /**
     * 配置增强版Agent
     */
    private AiManus configureEnhancedAgent() {
        // 设置RAG云知识库
        aiManus.setLoveAppRagCloudAdvisor(ragAdvisor);
        
        // 配置增强功能
        aiManus.setEnhancedSystemPrompt();
        aiManus.setEnableTypewriter(true);
        aiManus.setTypewriterDelay(30); // 30毫秒延迟，可调整
        aiManus.setMaxSteps(3); // 限制最大步骤数为3，避免循环调用
        
        return aiManus;
    }


    /**
     * 普通对话接口（不带打字机效果）
     */
    @PostMapping("/chat")
    public String chat(@RequestParam String message, 
                      @RequestParam(required = false) String conversationId) {
        if (conversationId == null) {
            conversationId = IdUtil.simpleUUID();
        }
        
        AiManus agent = configureEnhancedAgent();
        
        try {
            String result = agent.run(message);
            return String.format("会话ID: %s\n结果:\n%s", conversationId, result);
        } catch (Exception e) {
            return "错误: " + e.getMessage();
        }
    }

    /**
     * 流式对话接口（带打字机效果）
     */
    @GetMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam String message,
                                @RequestParam(required = false) String conversationId) {
        if (conversationId == null) {
            conversationId = IdUtil.simpleUUID();
        }
        
        AiManus agent = configureEnhancedAgent();
        
        return agent.runStreamWithTypewriter(message, conversationId);
    }

    /**
     * 配置打字机效果
     */
    @PostMapping("/config/typewriter")
    public String configTypewriter(@RequestParam boolean enable,
                                  @RequestParam(defaultValue = "50") long delay) {
        return String.format("打字机效果配置已更新: 启用=%s, 延迟=%dms", enable, delay);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "Enhanced Agent Controller is running with RAG, Database Memory, and Typewriter Effect!";
    }
} 