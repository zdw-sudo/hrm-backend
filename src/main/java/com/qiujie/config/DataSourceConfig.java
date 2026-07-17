package com.qiujie.config;

import org.activiti.spring.boot.ProcessEngineConfigurationConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean("activitiDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.activiti")
    public DataSource activitiDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public ProcessEngineConfigurationConfigurer activitiProcessEngineConfigurer(
            @Qualifier("activitiDataSource") DataSource activitiDataSource) {
        return processEngineConfiguration -> processEngineConfiguration.setDataSource(activitiDataSource);
    }
}
