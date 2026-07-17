package com.qiujie.ai.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AiChatRequest {

    private String message;

    private List<AiChatMessage> history = new ArrayList<>();
}
