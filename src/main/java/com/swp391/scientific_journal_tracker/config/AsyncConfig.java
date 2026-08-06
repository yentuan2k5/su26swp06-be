package com.swp391.scientific_journal_tracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Chỉ chạy một backfill cùng lúc để không làm Railway/MySQL free bị quá tải.
     */
    @Bean(name = "openAlexBackfillTaskExecutor")
    public Executor openAlexBackfillTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("openalex-backfill-");
        executor.initialize();

        return executor;
    }
}
