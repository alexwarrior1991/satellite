package com.alejandro.satellite.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for virtual threads.
 */
@Configuration
public class ThreadConfig {

    @Value("${app.telemetry.processing.max-concurrent-threads:1000}")
    private int maxConcurrentThreads;

    /**
     * Creates an executor service using virtual threads.
     * Virtual threads are lightweight threads that are managed by the JVM rather than the OS,
     * allowing for a much larger number of concurrent threads.
     *
     * @return an executor service using virtual threads
     */
    @Bean
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}