package com.cosmeticsshop.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatRateLimitService {

    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final long WINDOW_SECONDS = 60L;

    private final Map<String, Deque<Instant>> requestsByClient = new ConcurrentHashMap<>();

    public boolean allow(String clientKey) {
        String normalizedKey = clientKey == null || clientKey.isBlank() ? "anonymous" : clientKey;
        Deque<Instant> timestamps = requestsByClient.computeIfAbsent(normalizedKey, ignored -> new ArrayDeque<>());
        Instant now = Instant.now();

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(now.minusSeconds(WINDOW_SECONDS))) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }
}
