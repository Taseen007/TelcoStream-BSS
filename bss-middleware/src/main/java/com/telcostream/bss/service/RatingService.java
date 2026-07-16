package com.telcostream.bss.service;

import com.telcostream.bss.model.ServiceType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The charging engine: pure tariff math, no I/O. Kept side-effect free so
 * it's trivial to unit test (see RatingServiceTest).
 */
@Service
public class RatingService {

    private final BigDecimal voicePerMinute;
    private final BigDecimal smsPerMessage;
    private final BigDecimal dataPerMb;

    public RatingService(
            @Value("${bss.rating.voice-per-minute}") double voicePerMinute,
            @Value("${bss.rating.sms-per-message}") double smsPerMessage,
            @Value("${bss.rating.data-per-mb}") double dataPerMb) {
        this.voicePerMinute = BigDecimal.valueOf(voicePerMinute);
        this.smsPerMessage = BigDecimal.valueOf(smsPerMessage);
        this.dataPerMb = BigDecimal.valueOf(dataPerMb);
    }

    public BigDecimal rate(ServiceType serviceType, Integer durationSeconds, Double dataVolumeMb) {
        return switch (serviceType) {
            case VOICE -> rateVoice(durationSeconds);
            case SMS -> smsPerMessage;
            case DATA -> rateData(dataVolumeMb);
            case UNKNOWN -> throw new IllegalArgumentException("Cannot rate an UNKNOWN service type");
        };
    }

    private BigDecimal rateVoice(Integer durationSeconds) {
        if (durationSeconds == null || durationSeconds <= 0) {
            return BigDecimal.ZERO;
        }
        // Bill per second, pro-rated from the per-minute rate, rounded to 4 dp.
        BigDecimal minutes = BigDecimal.valueOf(durationSeconds)
                .divide(BigDecimal.valueOf(60), 6, RoundingMode.HALF_UP);
        return voicePerMinute.multiply(minutes).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal rateData(Double dataVolumeMb) {
        if (dataVolumeMb == null || dataVolumeMb <= 0) {
            return BigDecimal.ZERO;
        }
        return dataPerMb.multiply(BigDecimal.valueOf(dataVolumeMb)).setScale(4, RoundingMode.HALF_UP);
    }
}
