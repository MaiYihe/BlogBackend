package com.maihehe.blogcore._01_config.Rag;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RagAsyncConfig {

    @Bean(name = "ragTaskExecutor")
    public ThreadPoolTaskExecutor ragTaskExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(4);
        exec.setMaxPoolSize(8);
        exec.setQueueCapacity(100);
        exec.setThreadNamePrefix("rag-");
        exec.initialize();
        return exec;
    }
}
