package com.qiujie.ai.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 根据 hrm.ai.enabled 切换 Spring AI 模型开关，避免无 API Key 时启动失败。
 */
public class AiAutoConfigurationEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean enabled = environment.getProperty("hrm.ai.enabled", Boolean.class, false);
        if (enabled) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        props.put("spring.ai.model.chat", "none");
        props.put("spring.ai.model.embedding", "none");
        props.put("spring.ai.model.image", "none");
        props.put("spring.ai.model.moderation", "none");
        props.put("spring.ai.model.audio.speech", "none");
        props.put("spring.ai.model.audio.transcription", "none");

        environment.getPropertySources().addFirst(new MapPropertySource("hrmAiModelToggle", props));
    }
}
