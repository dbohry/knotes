package com.lhamacorp.knotes.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
            new CaffeineCache("contentCache", contentCacheCaffeine().build()),
            new CaffeineCache("metadataCache", metadataCacheCaffeine().build())
        ));

        return cacheManager;
    }

    @Bean
    public Caffeine<Object, Object> contentCacheCaffeine() {
        return Caffeine.newBuilder()
                .initialCapacity(20)
                .maximumSize(100)
                .expireAfterWrite(Duration.ofSeconds(30))
                .recordStats();
    }

    @Bean
    public Caffeine<Object, Object> metadataCacheCaffeine() {
        return Caffeine.newBuilder()
                .initialCapacity(50)
                .maximumSize(500)
                .expireAfterWrite(Duration.ofSeconds(10))
                .recordStats();
    }
}