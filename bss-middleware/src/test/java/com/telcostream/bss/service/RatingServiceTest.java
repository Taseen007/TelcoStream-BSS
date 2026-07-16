package com.telcostream.bss.service;

import com.telcostream.bss.model.ServiceType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RatingServiceTest {

    private final RatingService ratingService = new RatingService(1.20, 0.30, 0.50);

    @Test
    void ratesVoiceProRatedPerSecond() {
        // 60 seconds = exactly 1 minute -> 1.20 taka
        BigDecimal charge = ratingService.rate(ServiceType.VOICE, 60, null);
        assertThat(charge).isEqualByComparingTo("1.2000");
    }

    @Test
    void ratesVoiceForPartialMinute() {
        // 30 seconds = half a minute -> 0.60 taka
        BigDecimal charge = ratingService.rate(ServiceType.VOICE, 30, null);
        assertThat(charge).isEqualByComparingTo("0.6000");
    }

    @Test
    void ratesSmsAtFlatFee() {
        BigDecimal charge = ratingService.rate(ServiceType.SMS, 0, null);
        assertThat(charge).isEqualByComparingTo("0.3000");
    }

    @Test
    void ratesDataPerMb() {
        BigDecimal charge = ratingService.rate(ServiceType.DATA, null, 10.0);
        assertThat(charge).isEqualByComparingTo("5.0000");
    }

    @Test
    void zeroDurationVoiceCallIsFree() {
        BigDecimal charge = ratingService.rate(ServiceType.VOICE, 0, null);
        assertThat(charge).isEqualByComparingTo("0");
    }

    @Test
    void rejectsUnknownServiceType() {
        try {
            ratingService.rate(ServiceType.UNKNOWN, 10, null);
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // pass
        }
    }
}
