package it.ute.QAUTE.configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {
    @Bean
    public Cache<String, Map<String, Integer>> securityLimiterCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build();
    }
    @Bean
    public Cache<String, Long> temporaryLockCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .recordStats()
                .build();
    }
}
