package com.qiujie.ai.service;

import com.alibaba.fastjson.JSON;
import com.qiujie.ai.config.AiProperties;
import com.qiujie.ai.dto.AiChatMessage;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.exception.ServiceException;
import com.qiujie.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "hrm.ai", name = "enabled", havingValue = "true")
public class AiChatHistoryService {

    private static final String KEY_PREFIX = "ai:chat:history:";

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AiProperties aiProperties;

    public String requireStaffCode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ServiceException(BusinessStatusEnum.UNAUTHORIZED);
        }
        String staffCode = authentication.getName();
        if (staffCode == null || staffCode.isBlank()) {
            throw new ServiceException(BusinessStatusEnum.UNAUTHORIZED);
        }
        return staffCode;
    }

    public List<AiChatMessage> loadHistoryForCurrentUser() {
        return loadHistory(requireStaffCode());
    }

    public void clearHistoryForCurrentUser() {
        clearHistory(requireStaffCode());
    }

    public List<AiChatMessage> loadHistory(String staffCode) {
        Object raw = redisUtil.get(buildKey(staffCode));
        if (raw == null) {
            return new ArrayList<>();
        }
        List<AiChatMessage> messages = JSON.parseArray(String.valueOf(raw), AiChatMessage.class);
        return messages != null ? messages : new ArrayList<>();
    }

    public void appendTurn(String staffCode, String userMessage, String assistantReply) {
        List<AiChatMessage> history = loadHistory(staffCode);
        history.add(buildMessage("user", userMessage));
        history.add(buildMessage("assistant", assistantReply));
        saveHistory(staffCode, trimHistory(history));
    }

    public void clearHistory(String staffCode) {
        redisTemplate.delete(buildKey(staffCode));
    }

    private void saveHistory(String staffCode, List<AiChatMessage> history) {
        long ttlSeconds = aiProperties.getHistoryTtlDays() * 24L * 60L * 60L;
        redisUtil.set(buildKey(staffCode), JSON.toJSONString(history), ttlSeconds);
    }

    private List<AiChatMessage> trimHistory(List<AiChatMessage> history) {
        int maxMessages = Math.max(aiProperties.getHistoryMaxMessages(), 2);
        if (history.size() <= maxMessages) {
            return history;
        }
        return new ArrayList<>(history.subList(history.size() - maxMessages, history.size()));
    }

    private AiChatMessage buildMessage(String role, String content) {
        AiChatMessage message = new AiChatMessage();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    private String buildKey(String staffCode) {
        return KEY_PREFIX + staffCode;
    }
}
