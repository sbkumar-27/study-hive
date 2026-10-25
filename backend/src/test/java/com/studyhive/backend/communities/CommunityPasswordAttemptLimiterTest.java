package com.studyhive.backend.communities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CommunityPasswordAttemptLimiterTest {

    @Test
    void sixthAttemptWithinWindow_isBlocked_withoutNeedingAnotherFailureRecorded() {
        CommunityPasswordAttemptLimiter limiter = new CommunityPasswordAttemptLimiter(Duration.ofMinutes(15));

        for (int i = 0; i < 5; i++) {
            assertThat(limiter.isBlocked(1L, 100L)).isFalse();
            limiter.recordFailure(1L, 100L);
        }

        // 5 failures recorded; the 6th attempt must be rejected before even checking the password.
        assertThat(limiter.isBlocked(1L, 100L)).isTrue();
    }

    @Test
    void attemptsOutsideTheWindow_areForgotten() throws InterruptedException {
        CommunityPasswordAttemptLimiter limiter = new CommunityPasswordAttemptLimiter(Duration.ofMillis(50));

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(1L, 100L);
        }
        assertThat(limiter.isBlocked(1L, 100L)).isTrue();

        Thread.sleep(100);

        assertThat(limiter.isBlocked(1L, 100L)).isFalse();
    }

    @Test
    void successfulMatch_clearsHistory_soItNeverBlocksALaterLegitimateAttempt() {
        CommunityPasswordAttemptLimiter limiter = new CommunityPasswordAttemptLimiter(Duration.ofMinutes(15));

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(1L, 100L);
        }
        assertThat(limiter.isBlocked(1L, 100L)).isTrue();

        limiter.clear(1L, 100L);

        assertThat(limiter.isBlocked(1L, 100L)).isFalse();
    }

    @Test
    void differentKeys_areTrackedIndependently() {
        CommunityPasswordAttemptLimiter limiter = new CommunityPasswordAttemptLimiter(Duration.ofMinutes(15));

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(1L, 100L);
        }

        assertThat(limiter.isBlocked(1L, 100L)).isTrue();
        assertThat(limiter.isBlocked(2L, 100L)).isFalse();
        assertThat(limiter.isBlocked(1L, 200L)).isFalse();
    }
}
