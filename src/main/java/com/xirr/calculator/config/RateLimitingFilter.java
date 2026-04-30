package com.xirr.calculator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int CLEANUP_INTERVAL = 200;
    private static final int EXPIRY_WINDOW_MULTIPLIER = 3;

    private final AppRateLimitProperties rateLimitProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemUTC();
    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger requestCounter = new AtomicInteger();

    public RateLimitingFilter(AppRateLimitProperties rateLimitProperties, ObjectMapper objectMapper) {
        this.rateLimitProperties = rateLimitProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !rateLimitProperties.enabled() || matchRule(request) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        MatchedRule matchedRule = matchRule(request);
        if (matchedRule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long nowMillis = clock.millis();
        String clientKey = matchedRule.name() + ":" + resolveClientIp(request);
        TokenBucket bucket = buckets.computeIfAbsent(clientKey, ignored -> new TokenBucket(matchedRule.rule(), nowMillis));
        RateLimitDecision decision = bucket.tryConsume(nowMillis);

        response.setHeader("X-RateLimit-Limit", String.valueOf(matchedRule.rule().maxRequests()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remainingTokens()));

        if (!decision.allowed()) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            writeRateLimitResponse(request, response, decision.retryAfterSeconds(), matchedRule.name());
            triggerCleanup(nowMillis);
            return;
        }

        triggerCleanup(nowMillis);
        filterChain.doFilter(request, response);
    }

    private MatchedRule matchRule(HttpServletRequest request) {
        String method = request.getMethod();
        String servletPath = resolvePath(request);

        if ("POST".equalsIgnoreCase(method) && "/login".equals(servletPath)) {
            return new MatchedRule("login", rateLimitProperties.login());
        }

        if ("POST".equalsIgnoreCase(method) && "/access-request".equals(servletPath)) {
            return new MatchedRule("access request", rateLimitProperties.accessRequest());
        }

        if ("POST".equalsIgnoreCase(method) && "/api/xirr/calculate".equals(servletPath)) {
            return new MatchedRule("xirr", rateLimitProperties.xirr());
        }

        return null;
    }

    private String resolvePath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        if (servletPath != null && !servletPath.isBlank()) {
            return servletPath;
        }
        return request.getRequestURI();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private void writeRateLimitResponse(HttpServletRequest request,
                                        HttpServletResponse response,
                                        long retryAfterSeconds,
                                        String ruleName) throws IOException {
        String message = "Too many " + ruleName + " requests from your network. Please try again in "
                + retryAfterSeconds + " seconds.";

        if (resolvePath(request).startsWith("/api/")) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("message", message);
            payload.put("retryAfterSeconds", retryAfterSeconds);
            objectMapper.writeValue(response.getWriter(), payload);
            return;
        }

        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write(message);
    }

    private void triggerCleanup(long nowMillis) {
        if (requestCounter.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }

        buckets.entrySet().removeIf(entry -> entry.getValue().isExpired(nowMillis));
    }

    private record MatchedRule(String name, AppRateLimitProperties.RateLimitRule rule) {
    }

    private record RateLimitDecision(boolean allowed, int remainingTokens, long retryAfterSeconds) {
    }

    private static final class TokenBucket {

        private final int capacity;
        private final long windowMillis;
        private double availableTokens;
        private long lastRefillAtMillis;
        private long lastSeenAtMillis;

        private TokenBucket(AppRateLimitProperties.RateLimitRule rule, long nowMillis) {
            this.capacity = rule.maxRequests();
            this.windowMillis = rule.windowSeconds() * 1000L;
            this.availableTokens = rule.maxRequests();
            this.lastRefillAtMillis = nowMillis;
            this.lastSeenAtMillis = nowMillis;
        }

        private synchronized RateLimitDecision tryConsume(long nowMillis) {
            refill(nowMillis);
            lastSeenAtMillis = nowMillis;

            if (availableTokens >= 1) {
                availableTokens -= 1;
                return new RateLimitDecision(true, (int) Math.floor(availableTokens), 0);
            }

            long retryAfterMillis = Math.max(1, Math.round((1 - availableTokens) * windowMillis / capacity));
            long retryAfterSeconds = Math.max(1, (long) Math.ceil(retryAfterMillis / 1000.0d));
            return new RateLimitDecision(false, 0, retryAfterSeconds);
        }

        private synchronized boolean isExpired(long nowMillis) {
            return nowMillis - lastSeenAtMillis > (windowMillis * EXPIRY_WINDOW_MULTIPLIER);
        }

        private void refill(long nowMillis) {
            long elapsedMillis = nowMillis - lastRefillAtMillis;
            if (elapsedMillis <= 0) {
                return;
            }

            double tokensToAdd = (elapsedMillis * (double) capacity) / windowMillis;
            availableTokens = Math.min(capacity, availableTokens + tokensToAdd);
            lastRefillAtMillis = nowMillis;
        }
    }
}
