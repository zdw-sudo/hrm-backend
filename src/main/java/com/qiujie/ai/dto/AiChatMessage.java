package com.qiujie.ai.dto;

import lombok.Data;

@Data
public class AiChatMessage {

    /** user 或 assistant */
    private String role;

    private String content;
}
