package com.qiujie.ai.service;

import com.qiujie.ai.config.AiProperties;
import com.qiujie.ai.dto.AiChatMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "hrm.ai", name = "enabled", havingValue = "true")
public class AiAssistantService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    private AiProperties aiProperties;

    @Autowired
    private AiChatHistoryService chatHistoryService;

    @Autowired
    @Qualifier("hrChatClient")
    private ChatClient hrChatClient;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    public String chat(String userMessage) {
        String validationError = validate(userMessage);
        if (validationError != null) {
            return validationError;
        }

        String staffCode = chatHistoryService.requireStaffCode();
        String trimmedMessage = userMessage.trim();
        List<AiChatMessage> history = chatHistoryService.loadHistory(staffCode);
        String reply = preparePrompt(history)
                .user(trimmedMessage)
                .call()
                .content();
        chatHistoryService.appendTurn(staffCode, trimmedMessage, reply);
        return reply;
    }

    public Flux<String> streamChat(String userMessage) {
        String validationError = validate(userMessage);
        if (validationError != null) {
            return Flux.just(validationError);
        }

        String staffCode = chatHistoryService.requireStaffCode();
        String trimmedMessage = userMessage.trim();
        List<AiChatMessage> history = chatHistoryService.loadHistory(staffCode);
        StringBuilder fullReply = new StringBuilder();

        return preparePrompt(history)
                .user(trimmedMessage)
                .stream()
                .content()
                .doOnNext(fullReply::append)
                .doOnComplete(() -> {
                    if (!fullReply.isEmpty()) {
                        chatHistoryService.appendTurn(staffCode, trimmedMessage, fullReply.toString());
                    }
                });
    }

    private ChatClient.ChatClientRequestSpec preparePrompt(List<AiChatMessage> history) {
        LocalDate today = LocalDate.now();
        List<Message> historyMessages = toSpringMessages(history);
        ChatClient.ChatClientRequestSpec spec = hrChatClient.prompt()
                .system(prompt -> prompt
                        .param("currentDate", today.format(DATE_FORMAT))
                        .param("currentMonth", today.format(MONTH_FORMAT)));
        if (!historyMessages.isEmpty()) {
            spec = spec.messages(historyMessages);
        }
        return spec;
    }

    private String validate(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "请输入您的问题。";
        }
        if (!aiProperties.isEnabled()) {
            return "AI 助手未启用。请在 application.yml 中设置 hrm.ai.enabled=true。";
        }
        if (apiKey == null || apiKey.isBlank()) {
            return "AI 助手未配置 API Key。请设置 spring.ai.openai.api-key 或环境变量 DEEPSEEK_API_KEY。";
        }
        return null;
    }

    private List<Message> toSpringMessages(List<AiChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        int maxMessages = Math.max(aiProperties.getHistoryMaxMessages(), 2);
        List<Message> messages = new ArrayList<>();
        int start = Math.max(0, history.size() - maxMessages);
        for (int i = start; i < history.size(); i++) {
            AiChatMessage item = history.get(i);
            if (item == null || item.getContent() == null || item.getContent().isBlank()) {
                continue;
            }
            String role = item.getRole() != null ? item.getRole().trim().toLowerCase() : "";
            String content = item.getContent().trim();
            switch (role) {
                case "user" -> messages.add(new UserMessage(content));
                case "assistant" -> messages.add(new AssistantMessage(content));
                default -> {
                }
            }
        }
        return messages;
    }
}
