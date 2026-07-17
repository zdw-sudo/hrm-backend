package com.qiujie.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "hrm.ai")
public class AiProperties {

    /**
     * 是否启用 AI 助手模块（需要同时配置 spring.ai.openai.api-key）
     */
    private boolean enabled = true;

    /**
     * 每个用户最多保留的对话条数（user + assistant 各算一条）
     */
    private int historyMaxMessages = 20;

    /**
     * Redis 对话记录过期天数
     */
    private int historyTtlDays = 7;
}
