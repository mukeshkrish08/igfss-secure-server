package registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks failed sign-in attempts and enforces a 3-strikes lockout policy
 * Each email is tracked independently inside a 15-minute
 * sliding window. A successful sign-in clears the count for that email.
 *
 * Thread-safe: ConcurrentHashMap for the map, AtomicInteger for the
 * counter, and `synchronized` on the public methods so check-then-act
 * sequences (check window expiry, then increment) stay atomic.
 */
public final class SignInAttemptLedger {

    /** Three attempts before the account is locked. */
    public static final int MAX_ATTEMPTS = 3;

    // 15 minutes - long enough to deter brute force, short enough that
    // a legitimate user who fat-fingered the password isn't stuck out.
    private static final long ATTEMPT_RESET_MILLIS = 15L * 60L * 1000L;

    private static class AttemptRecord {
        final AtomicInteger failureCount = new AtomicInteger(0);
        volatile long windowStartMillis;
    }

    private static final Map<String, AttemptRecord> attemptsByEmail = new ConcurrentHashMap<>();

    private SignInAttemptLedger() {
    }

    /**
     * Records a failed attempt and returns how many tries are still left.
     * Zero return value means the account is now locked for this window.
     */
    public static synchronized int registerFailure(String loginEmail) {
        AttemptRecord record = attemptsByEmail.computeIfAbsent(loginEmail, k -> new AttemptRecord());
        long now = System.currentTimeMillis();

        // If the previous window has expired (or this is the first
        // failure), open a fresh window rather than carrying old counts
        // forward - otherwise a typo from an hour ago shouldn't push
        // someone past the limit on their next attempt.
        if (record.failureCount.get() == 0
                || (now - record.windowStartMillis) > ATTEMPT_RESET_MILLIS) {
            record.windowStartMillis = now;
            record.failureCount.set(0);
        }

        int newCount = record.failureCount.incrementAndGet();
        return Math.max(0, MAX_ATTEMPTS - newCount);
    }

    /**
     * Returns true if this email is currently locked. Auto-resets the
     * counter if the lockout window has expired since the last failure.
     */
    public static synchronized boolean isLocked(String loginEmail) {
        AttemptRecord record = attemptsByEmail.get(loginEmail);
        if (record == null) return false;
        long now = System.currentTimeMillis();
        if ((now - record.windowStartMillis) > ATTEMPT_RESET_MILLIS) {
            record.failureCount.set(0);
            return false;
        }
        return record.failureCount.get() >= MAX_ATTEMPTS;
    }

    /** Called after a successful sign-in so the next session starts fresh. */
    public static synchronized void clearFailures(String loginEmail) {
        attemptsByEmail.remove(loginEmail);
    }
}
