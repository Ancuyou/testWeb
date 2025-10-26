package it.ute.QAUTE.configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import it.ute.QAUTE.dto.response.MFAResponse;
import it.ute.QAUTE.service.AccountService;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {
    @Autowired
    private MFARemovalListener mfaRemovalListener;
    @Autowired
    @Lazy
    private AccountService accountService;
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
    @Bean
    public Cache<String, MFAResponse> temporaryMFACache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .recordStats()
                .removalListener(mfaRemovalListener)
                .build();
    }
    @Bean
    public Cache<Integer, Boolean> onlineCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .recordStats()
                .removalListener((Integer userId, Boolean status, RemovalCause cause) -> {
                    if (userId != null) {
                        accountService.updateAccountOffline(userId);
                        System.out.println("❌ User " + userId + " bị remove khỏi cache. Lý do: " + cause);
                    }
                })
                .build();
    }
}
