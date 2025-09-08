package com.inventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.sql.SQLException;
import java.util.Collections;

@Configuration
@EnableRetry
public class SqliteRetryConfig {

    @Bean
    public RetryTemplate sqliteRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // Retry policy - retry up to 3 times on SQLite busy errors
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(3, 
            Collections.singletonMap(SQLException.class, true));
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // Exponential backoff - start with 100ms, max 1000ms
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(100);
        backOffPolicy.setMaxInterval(1000);
        backOffPolicy.setMultiplier(2.0);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        // Add listener for debugging
        retryTemplate.setListeners(new RetryListener[]{
            new RetryListener() {
                @Override
                public <T, E extends Throwable> void onError(RetryContext context, 
                        RetryCallback<T, E> callback, Throwable throwable) {
                    if (throwable.getMessage() != null && 
                        throwable.getMessage().contains("database is locked")) {
                        System.out.println("🔄 SQLite retry attempt " + context.getRetryCount() + 
                                         " due to lock: " + throwable.getMessage());
                    }
                }
            }
        });
        
        return retryTemplate;
    }
}