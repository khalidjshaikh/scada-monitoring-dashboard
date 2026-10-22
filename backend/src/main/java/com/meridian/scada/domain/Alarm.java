package com.meridian.scada.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "scada_alarm", indexes = {
        @Index(name = "idx_alarm_status", columnList = "status"),
        @Index(name = "idx_alarm_rule_key", columnList = "ruleKey"),
        @Index(name = "idx_alarm_raised_at", columnList = "raisedAt")
})
public class Alarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlarmSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlarmStatus status;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 500)
    private String message;

    private Long siteId;

    @Column(length = 16)
    private String siteCode;

    @Column(length = 120)
    private String siteName;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(nullable = false, length = 160)
    private String ruleKey;

    @Column(nullable = false)
    private Instant raisedAt;

    private Instant acknowledgedAt;

    @Column(length = 60)
    private String acknowledgedBy;

    private Instant clearedAt;

    protected Alarm() {
    }

    public Alarm(AlarmSeverity severity, AlarmStatus status, String code, String message,
                 Long siteId, String siteCode, String siteName, String source, String ruleKey,
                 Instant raisedAt) {
        this.severity = severity;
        this.status = status;
        this.code = code;
        this.message = message;
        this.siteId = siteId;
        this.siteCode = siteCode;
        this.siteName = siteName;
        this.source = source;
        this.ruleKey = ruleKey;
        this.raisedAt = raisedAt;
    }

    public Long getId() {
        return id;
    }

    public AlarmSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlarmSeverity severity) {
        this.severity = severity;
    }

    public AlarmStatus getStatus() {
        return status;
    }

    public void setStatus(AlarmStatus status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getSiteId() {
        return siteId;
    }

    public String getSiteCode() {
        return siteCode;
    }

    public String getSiteName() {
        return siteName;
    }

    public String getSource() {
        return source;
    }

    public String getRuleKey() {
        return ruleKey;
    }

    public Instant getRaisedAt() {
        return raisedAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public void setAcknowledgedAt(Instant acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
    }

    public void setAcknowledgedBy(String acknowledgedBy) {
        this.acknowledgedBy = acknowledgedBy;
    }

    public Instant getClearedAt() {
        return clearedAt;
    }

    public void setClearedAt(Instant clearedAt) {
        this.clearedAt = clearedAt;
    }
}
