package com.mailSender.common.security;

import com.mailSender.common.exception.ApiException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class AuthRateLimiter {

  private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

  public void check(String key, int limitPerMinute) {
    long now = System.currentTimeMillis();
    long windowStart = now - 60_000;
    Deque<Long> times = hits.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    synchronized (times) {
      while (!times.isEmpty() && times.peekFirst() < windowStart) {
        times.pollFirst();
      }
      if (times.size() >= limitPerMinute) {
        throw new ApiException(
            "RATE_LIMITED", "Too many authentication attempts. Try again later.", 429);
      }
      times.addLast(now);
    }
  }
}
