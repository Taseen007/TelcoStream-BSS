package com.telcostream.bss.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Raw CDR as produced by the network / generator. Fields are deliberately
 * loose (Strings, boxed types) because this is untrusted external input —
 * validation and coercion happen in CdrValidationService, not here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdrRecord {

    private String recordId;
    private String callingNumber;
    private String calledNumber;
    private Integer durationSeconds;
    private Double dataVolumeMb;
    private String timestamp;
    private String serviceType;
    private String locationCellId;

    public CdrRecord() {
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getCallingNumber() {
        return callingNumber;
    }

    public void setCallingNumber(String callingNumber) {
        this.callingNumber = callingNumber;
    }

    public String getCalledNumber() {
        return calledNumber;
    }

    public void setCalledNumber(String calledNumber) {
        this.calledNumber = calledNumber;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Double getDataVolumeMb() {
        return dataVolumeMb;
    }

    public void setDataVolumeMb(Double dataVolumeMb) {
        this.dataVolumeMb = dataVolumeMb;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getLocationCellId() {
        return locationCellId;
    }

    public void setLocationCellId(String locationCellId) {
        this.locationCellId = locationCellId;
    }

    @Override
    public String toString() {
        return "CdrRecord{recordId='%s', callingNumber='%s', serviceType='%s'}"
                .formatted(recordId, callingNumber, serviceType);
    }
}
