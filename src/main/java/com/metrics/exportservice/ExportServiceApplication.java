package com.metrics.exportservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
public class ExportServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExportServiceApplication.class, args);
    }

    @Bean(name = "bulkProcessingExecutor")
    public Executor bulkProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("BulkProcessing-");
        executor.initialize();
        return executor;
    }
}
