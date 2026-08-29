package com.bitalep.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;

    public AuthRateLimitFilter(com.bitalep.config.AppProperties props) {
        this.limit = props.rateLimit().authPerMinute();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !("/api/auth/login".equals(path) || "/api/auth/register".equals(path)
                || "/api/auth/forgot-password".equals(path));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String ip = clientIp(request);
        Window w = windows.compute(ip, (k, existing) -> {
            Instant now = Instant.now();
            if (existing == null || existing.windowStart.plusSeconds(60).isBefore(now)) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        if (w.count.get() > limit) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":{\"code\":\"RATE_LIMITED\",\"message\":\"errors:rateLimited\"}}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Window(Instant windowStart, AtomicInteger count) {}
}
