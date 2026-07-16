package com.telcostream.bss.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "record_id", nullable = false, unique = true)
    private String recordId;

    @Column(name = "calling_number", nullable = false)
    private String callingNumber;

    @Column(name = "called_number")
    private String calledNumber;

    @Column(name = "service_type", nullable = false)
    private String serviceType;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "charged_amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal chargedAmount;

    @Column(name = "location_cell_id")
    private String locationCellId;

    @Column(name = "event_timestamp", nullable = false)
    private Instant eventTimestamp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Invoice() {
    }

    public Invoice(String recordId, String callingNumber, String calledNumber, String serviceType,
                    Integer durationSeconds, BigDecimal chargedAmount, String locationCellId,
                    Instant eventTimestamp) {
        this.recordId = recordId;
        this.callingNumber = callingNumber;
        this.calledNumber = calledNumber;
        this.serviceType = serviceType;
        this.durationSeconds = durationSeconds;
        this.chargedAmount = chargedAmount;
        this.locationCellId = locationCellId;
        this.eventTimestamp = eventTimestamp;
    }

    public Long getId() {
        return id;
    }

    public String getRecordId() {
        return recordId;
    }

    public String getCallingNumber() {
        return callingNumber;
    }

    public String getCalledNumber() {
        return calledNumber;
    }

    public String getServiceType() {
        return serviceType;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public BigDecimal getChargedAmount() {
        return chargedAmount;
    }

    public String getLocationCellId() {
        return locationCellId;
    }

    public Instant getEventTimestamp() {
        return eventTimestamp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
