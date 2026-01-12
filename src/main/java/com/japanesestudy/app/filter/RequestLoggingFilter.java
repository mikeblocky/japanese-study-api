package com.japanesestudy.app.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * API Gateway-style request logging filter.
 * Logs request details, timing, and user info for observability.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String userId = getUserId();
            
            // Log in a structured format for easy parsing
            if (log.isInfoEnabled() && !uri.contains("/actuator")) {
                log.info("API {} {} {} - {} - {}ms - user:{}",
                        method,
                        uri,
                        queryString != null ? "?" + queryString : "",
                        status,
                        duration,
                        userId != null ? userId : "anonymous");
            }
            
            // Add gateway-style headers
            response.setHeader("X-Response-Time", duration + "ms");
            response.setHeader("X-Request-Id", request.getHeader("X-Request-Id"));
        }
    }
    
    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return null;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Don't log static resources or health checks
        String uri = request.getRequestURI();
        return uri.startsWith("/static") || 
               uri.startsWith("/favicon") ||
               uri.equals("/");
    }
}
