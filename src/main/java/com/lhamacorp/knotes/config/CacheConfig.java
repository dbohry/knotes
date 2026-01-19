package com.lhamacorp.knotes.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.time.Duration.ofSeconds;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache content = build("content", ofSeconds(60), 1000);
        CaffeineCache metadata = build("metadata", ofSeconds(10), 500);
        CaffeineCache current = build("current", ofSeconds(60), 1000);

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(content, metadata, current));
        return manager;
    }

    private CaffeineCache build(String name, Duration duration, long size) {
        return new CaffeineCache(name, newBuilder()
                .expireAfterWrite(duration)
                .maximumSize(size)
                .recordStats()
                .build());
    }

}