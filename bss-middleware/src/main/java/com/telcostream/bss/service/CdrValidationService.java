package com.telcostream.bss.service;

import com.telcostream.bss.model.CdrRecord;
import com.telcostream.bss.model.ServiceType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * First line of defense against garbage/fraudulent input. Never throws for
 * bad data — returns an empty Optional so the caller can route the record to
 * the DLQ instead of crashing the consumer thread.
 */
@Service
public class CdrValidationService {

    private static final int MAX_CALL_DURATION_SECONDS = 24 * 60 * 60; // 1 day, generous outlier ceiling
    private static final double MAX_DATA_VOLUME_MB = 100_000; // ~100GB in one record is not plausible

    public record ValidatedCdr(String recordId, String callingNumber, String calledNumber,
                                ServiceType serviceType, Integer durationSeconds, Double dataVolumeMb,
                                String locationCellId, Instant eventTimestamp) {
    }

    public Optional<ValidatedCdr> validate(CdrRecord raw) {
        if (raw == null) {
            return Optional.empty();
        }
        if (isBlank(raw.getRecordId()) || isBlank(raw.getCallingNumber())) {
            return Optional.empty();
        }

        ServiceType serviceType = parseServiceType(raw.getServiceType());
        if (serviceType == ServiceType.UNKNOWN) {
            return Optional.empty();
        }

        Instant eventTimestamp = parseTimestamp(raw.getTimestamp());
        if (eventTimestamp == null) {
            return Optional.empty();
        }

        if (serviceType == ServiceType.DATA) {
            Double volume = raw.getDataVolumeMb();
            if (volume == null || volume <= 0 || volume > MAX_DATA_VOLUME_MB) {
                return Optional.empty();
            }
        } else {
            Integer duration = raw.getDurationSeconds();
            if (duration == null || duration < 0 || duration > MAX_CALL_DURATION_SECONDS) {
                return Optional.empty();
            }
        }

        return Optional.of(new ValidatedCdr(
                raw.getRecordId(),
                raw.getCallingNumber(),
                raw.getCalledNumber(),
                serviceType,
                raw.getDurationSeconds(),
                raw.getDataVolumeMb(),
                raw.getLocationCellId(),
                eventTimestamp
        ));
    }

    private ServiceType parseServiceType(String value) {
        if (isBlank(value)) {
            return ServiceType.UNKNOWN;
        }
        try {
            return ServiceType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ServiceType.UNKNOWN;
        }
    }

    private Instant parseTimestamp(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
