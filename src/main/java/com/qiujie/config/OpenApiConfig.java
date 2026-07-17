package com.qiujie.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hrmOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("人力资源管理系统 API")
                        .description("人力资源管理系统 API 文档")
                        .version("3.0"));
    }
}
