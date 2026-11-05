package com.reliefgrid.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

/** Defense-in-depth headers for a JSON API (no inline HTML is served, so CSP is intentionally strict). */
@Configuration
public class SecurityHeadersConfig {

    @Bean
    FilterRegistrationBean<?> securityHeadersFilter() {
        var filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                res.setHeader("X-Content-Type-Options", "nosniff");
                res.setHeader("X-Frame-Options", "DENY");
                res.setHeader("Referrer-Policy", "no-referrer");
                res.setHeader("Content-Security-Policy", "default-src 'none'");
                res.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
                if (req.isSecure()) {
                    res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
                }
                chain.doFilter(req, res);
            }
        };
        var reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }
}
