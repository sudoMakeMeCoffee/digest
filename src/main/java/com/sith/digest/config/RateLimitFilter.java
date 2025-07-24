package com.sith.digest.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private final Map<String, RequestBucket> ipBuckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String ip = request.getRemoteAddr();
        RequestBucket bucket = ipBuckets.computeIfAbsent(ip, k -> new RequestBucket());

        synchronized (bucket) {
            if (bucket.allowRequest()) {
                chain.doFilter(request, response);
            } else {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(429); // Too Many Requests
                httpResponse.getWriter().write("Rate limit exceeded. Try again later.");
            }
        }
    }

    static class RequestBucket {
        private static final long WINDOW_MS = 60 * 1000;
        private int requestCount = 0;
        private long windowStart = Instant.now().toEpochMilli();

        public boolean allowRequest() {
            long now = Instant.now().toEpochMilli();
            if (now - windowStart > WINDOW_MS) {
                requestCount = 0;
                windowStart = now;
            }
            return ++requestCount <= MAX_REQUESTS_PER_MINUTE;
        }
    }
}
