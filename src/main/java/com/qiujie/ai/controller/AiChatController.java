package com.qiujie.ai.controller;

import com.qiujie.ai.dto.AiChatRequest;
import com.qiujie.ai.scope.AiDataScopeService;
import com.qiujie.ai.scope.AiOperatorContext;
import com.qiujie.ai.service.AiAssistantService;
import com.qiujie.ai.service.AiChatHistoryService;
import com.qiujie.dto.Response;
import com.qiujie.dto.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/ai")
@ConditionalOnProperty(prefix = "hrm.ai", name = "enabled", havingValue = "true")
public class AiChatController {

    @Autowired
    private AiAssistantService aiAssistantService;

    @Autowired
    private AiChatHistoryService chatHistoryService;

    @Autowired
    private AiDataScopeService aiDataScopeService;

    @Operation(summary = "获取当前用户的 AI 数据查询范围")
    @PreAuthorize("hasAuthority('ai:chat')")
    @GetMapping("/scope")
    public ResponseDTO getScope() {
        AiOperatorContext operator = aiDataScopeService.getCurrentOperator();
        return Response.success(Map.of(
                "scope", operator.getScope().name(),
                "description", aiDataScopeService.describeCurrentScope(),
                "operator", operator.getCode(),
                "operatorName", operator.getName(),
                "deptName", operator.getDeptName() != null ? operator.getDeptName() : ""
        ));
    }

    @Operation(summary = "获取当前用户的 AI 对话历史（Redis）")
    @PreAuthorize("hasAuthority('ai:chat')")
    @GetMapping("/chat/history")
    public ResponseDTO getHistory() {
        return Response.success(chatHistoryService.loadHistoryForCurrentUser());
    }

    @Operation(summary = "清空当前用户的 AI 对话历史（Redis）")
    @PreAuthorize("hasAuthority('ai:chat')")
    @DeleteMapping("/chat/history")
    public ResponseDTO clearHistory() {
        chatHistoryService.clearHistoryForCurrentUser();
        return Response.success();
    }

    @Operation(summary = "HR 智能助手对话（Tool Calling + Redis 多轮上下文）")
    @PreAuthorize("hasAuthority('ai:chat')")
    @PostMapping("/chat")
    public ResponseDTO chat(@RequestBody AiChatRequest request) {
        String message = request != null ? request.getMessage() : null;
        String reply = aiAssistantService.chat(message);
        return Response.success(Map.of("reply", reply));
    }

    @Operation(summary = "HR 智能助手流式对话（SSE + Redis 多轮上下文）")
    @PreAuthorize("hasAuthority('ai:chat')")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody AiChatRequest request) {
        String message = request != null ? request.getMessage() : null;
        return aiAssistantService.streamChat(message);
    }
}
