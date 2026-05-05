package com.reports.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${app.cache.outstanding-dashboard-ttl-seconds:30}") int dashboardTtlSeconds
    ) {
        CaffeineCacheManager manager = new CaffeineCacheManager("outstandingDashboard");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(dashboardTtlSeconds, TimeUnit.SECONDS)
                .maximumSize(20_000));
        return manager;
    }

    /** Stable cache key from dashboard method arguments (user + filter dimensions). */
    @Bean("outstandingDashboardKeyGenerator")
    public KeyGenerator outstandingDashboardKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            for (Object p : params) {
                if (sb.length() > 0) {
                    sb.append('|');
                }
                sb.append(p != null ? p.toString() : "");
            }
            return sb.toString();
        };
    }
}
