package com.japanesestudy.app.config;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for improved performance. Uses in-memory cache for
 * frequently accessed data.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "courses",
                "courseById",
                "topicsByCourse",
                "itemsByTopic");

        cacheManager.setCacheSpecification(
                "maximumSize=10000,expireAfterWrite=" + Duration.ofMinutes(10).toSeconds() + "s,recordStats");

        return cacheManager;
    }
}
