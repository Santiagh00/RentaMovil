package com.rentamovil.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de rate limiting que limita el número de peticiones por IP.
 * Previene ataques de fuerza bruta y DoS.
 *
 * Límites por defecto:
 * - Login: 5 intentos por minuto
 * - Otros endpoints: 60 peticiones por minuto
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rate-limit.login.max-attempts:5}")
    private int maxLoginAttempts;

    @Value("${rate-limit.login.window-ms:60000}")
    private long loginWindowMs;

    @Value("${rate-limit.api.max-requests:60}")
    private int maxApiRequests;

    @Value("${rate-limit.api.window-ms:60000}")
    private long apiWindowMs;

    private final Map<String, RateLimitEntry> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, RateLimitEntry> apiRequests = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        // Rate limit específico para login
        if (path.contains("/api/auth/login")) {
            if (!checkRateLimit(clientIp, loginAttempts, maxLoginAttempts, loginWindowMs, "login")) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Demasiados intentos de login. Intenta más tarde.\"}");
                return;
            }
        } else {
            // Rate limit general para API
            if (!checkRateLimit(clientIp, apiRequests, maxApiRequests, apiWindowMs, "api")) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Demasiadas peticiones. Intenta más tarde.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean checkRateLimit(String key, Map<String, RateLimitEntry> store,
                                   int maxAttempts, long windowMs, String type) {
        long now = System.currentTimeMillis();
        RateLimitEntry entry = store.compute(key, (k, v) -> {
            if (v == null || now - v.windowStart > windowMs) {
                return new RateLimitEntry(now, new AtomicInteger(1));
            }
            v.count.incrementAndGet();
            return v;
        });

        boolean allowed = entry.count.get() <= maxAttempts;
        if (!allowed) {
            log.warn("Rate limit excedido para {} en {}: {} peticiones en {}ms",
                    key, type, entry.count.get(), windowMs);
        }
        return allowed;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count;

        RateLimitEntry(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}