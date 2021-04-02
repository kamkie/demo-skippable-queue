package com.example.demoskippablequeue.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {

    @Bean
    public ExecutorService workExecutor(@Value("worker.threads") int threads) {
        return Executors.newFixedThreadPool(threads, Executors.defaultThreadFactory());
    }
}
