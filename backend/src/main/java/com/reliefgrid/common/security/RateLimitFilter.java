package com.reliefgrid.common.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limits login/register/refresh to 10 requests per minute per client IP.
 * In-memory and per-instance: fine for a single-node deployment or a learning project.
 * Once the API runs behind Kubernetes with multiple replicas (Step 9), move this to a
 * Redis-backed Bucket4j configuration so the limit is shared across pods.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (isAuthEndpoint(req.getRequestURI())) {
            Bucket bucket = buckets.computeIfAbsent(clientKey(req), k -> newBucket());
            if (!bucket.tryConsume(1)) {
                res.setStatus(429);
                res.setContentType("application/problem+json");
                res.getWriter().write("""
                        {"status":429,"title":"Too Many Requests","detail":"Too many attempts. Please wait a moment and try again."}""");
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private boolean isAuthEndpoint(String uri) {
        return uri.startsWith("/api/v1/auth/login") || uri.startsWith("/api/v1/auth/register") || uri.startsWith("/api/v1/auth/refresh");
    }

    private Bucket newBucket() {
        return Bucket.builder().addLimit(Bandwidth.classic(10, io.github.bucket4j.Refill.intervally(10, Duration.ofMinutes(1)))).build();
    }

    /** X-Forwarded-For is trusted here only because the ingress/load balancer sets it, not the client. */
    private String clientKey(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
