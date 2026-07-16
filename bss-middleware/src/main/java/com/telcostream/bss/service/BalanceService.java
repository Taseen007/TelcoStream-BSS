package com.telcostream.bss.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * All prepaid-balance state lives in Redis for sub-millisecond reads/writes
 * at telecom scale. Also doubles as the idempotency guard: Kafka's
 * at-least-once delivery means the same CDR can arrive twice, so every
 * recordId is checked against a "processed" set before it's allowed to
 * touch a balance.
 */
@Service
public class BalanceService {

    private static final Logger log = LoggerFactory.getLogger(BalanceService.class);

    private static final String BALANCE_KEY_PREFIX = "balance:";
    private static final String PROCESSED_KEY_PREFIX = "processed-record:";
    private static final Duration PROCESSED_TTL = Duration.ofDays(2);

    private final StringRedisTemplate redisTemplate;

    public BalanceService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Returns true if this recordId has already been charged (i.e. this is a
     * Kafka redelivery, not a new event). Atomically marks it as seen.
     */
    public boolean isDuplicate(String recordId) {
        String key = PROCESSED_KEY_PREFIX + recordId;
        Boolean firstTimeSeen = redisTemplate.opsForValue().setIfAbsent(key, "1", PROCESSED_TTL);
        return firstTimeSeen == null || !firstTimeSeen;
    }

    public BigDecimal getBalance(String msisdn) {
        String value = redisTemplate.opsForValue().get(BALANCE_KEY_PREFIX + msisdn);
        return value == null ? BigDecimal.ZERO : new BigDecimal(value);
    }

    /**
     * Deducts `amount` from the subscriber's balance regardless of whether it
     * goes negative — real prepaid systems would typically block the call
     * instead, but for a portfolio project we charge and flag rather than
     * silently drop revenue-bearing events. Returns the resulting balance.
     */
    public BigDecimal deduct(String msisdn, BigDecimal amount) {
        String key = BALANCE_KEY_PREFIX + msisdn;
        // Use a simple read-modify-write; for true concurrency safety at
        // higher scale this would use a Lua script (atomic check-and-deduct).
        BigDecimal current = getBalance(msisdn);
        BigDecimal updated = current.subtract(amount);
        redisTemplate.opsForValue().set(key, updated.toPlainString());

        if (updated.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Subscriber {} balance went negative after charge: {}", msisdn, updated);
        }
        return updated;
    }
}
