package com.studyhive.backend.communities;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

/**
 * In-memory rate limiter for wrong community-password attempts, keyed by
 * {@code userId + ":" + communityId}. Allows at most {@link #MAX_ATTEMPTS}
 * failed attempts per key per rolling {@link #WINDOW}. A successful match
 * clears the key's history so a later legitimate attempt is never blocked by
 * stale failures.
 *
 * <p>This is a single-instance, in-memory limiter -- it does not survive a
 * restart and is not shared across multiple backend instances. That is an
 * accepted tradeoff for the current single-instance deployment.
 */
@Component
public class CommunityPasswordAttemptLimiter {

    static final int MAX_ATTEMPTS = 5;
    static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();
    private final Duration window;

    public CommunityPasswordAttemptLimiter() {
        this(DEFAULT_WINDOW);
    }

    /** Package-visible: lets tests use a much shorter window instead of waiting on real time. */
    CommunityPasswordAttemptLimiter(Duration window) {
        this.window = window;
    }

    /** Returns true when this key has already reached the failed-attempt limit for the current window. */
    public boolean isBlocked(Long userId, Long communityId) {
        Deque<Instant> attempts = attemptsByKey.get(key(userId, communityId));
        if (attempts == null) {
            return false;
        }
        return countRecent(attempts) >= MAX_ATTEMPTS;
    }

    /** Records a failed password attempt for this key. */
    public void recordFailure(Long userId, Long communityId) {
        Deque<Instant> attempts = attemptsByKey.computeIfAbsent(key(userId, communityId), k -> new ConcurrentLinkedDeque<>());
        attempts.addLast(Instant.now());
        countRecent(attempts); // opportunistically prune old entries
    }

    /** Clears this key's failed-attempt history, e.g. after a successful password match. */
    public void clear(Long userId, Long communityId) {
        attemptsByKey.remove(key(userId, communityId));
    }

    private int countRecent(Deque<Instant> attempts) {
        Instant cutoff = Instant.now().minus(window);
        while (true) {
            Instant oldest = attempts.peekFirst();
            if (oldest == null || !oldest.isBefore(cutoff)) {
                break;
            }
            attempts.pollFirst();
        }
        return attempts.size();
    }

    private static String key(Long userId, Long communityId) {
        return userId + ":" + communityId;
    }
}
