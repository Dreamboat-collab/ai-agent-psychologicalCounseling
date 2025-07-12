package com.aiagenttest.repository;


import com.aiagenttest.mapper.ChatMessageMapper;
import com.aiagenttest.model.ChatMessage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ChatMessageRepository extends ServiceImpl<ChatMessageMapper, ChatMessage> {
}
