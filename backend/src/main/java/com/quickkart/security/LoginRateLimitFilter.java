package com.quickkart.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final int maxAttemptsPerMinute;
    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(@Value("${app.security.login-rate-limit:25}") int maxAttemptsPerMinute) {
        this.maxAttemptsPerMinute = maxAttemptsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("POST".equalsIgnoreCase(request.getMethod())
                && ("/api/auth/login".equals(path) || "/api/auth/forgot-password".equals(path))) {
            String clientKey = resolveClientIp(request) + ":" + path;
            long nowEpochSec = Instant.now().getEpochSecond();
            AttemptWindow window = attempts.compute(clientKey, (k, existing) -> {
                if (existing == null || nowEpochSec - existing.windowStartSec >= 60) {
                    return new AttemptWindow(nowEpochSec, 1);
                }
                existing.count++;
                return existing;
            });
            if (window.count > maxAttemptsPerMinute) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many login attempts. Please wait a minute before trying again.\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class AttemptWindow {
        final long windowStartSec;
        int count;

        AttemptWindow(long windowStartSec, int count) {
            this.windowStartSec = windowStartSec;
            this.count = count;
        }
    }
}
