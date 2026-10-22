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
@Table(name = "scada_reading", indexes = {
        @Index(name = "idx_reading_time", columnList = "recordedAt"),
        @Index(name = "idx_reading_site_time", columnList = "siteId,recordedAt")
})
public class TelemetryReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long siteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Metric metric;

    @Column(name = "metric_value", nullable = false)
    private double value;

    @Column(nullable = false)
    private Instant recordedAt;

    protected TelemetryReading() {
    }

    public TelemetryReading(Long siteId, Metric metric, double value, Instant recordedAt) {
        this.siteId = siteId;
        this.metric = metric;
        this.value = value;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getSiteId() {
        return siteId;
    }

    public Metric getMetric() {
        return metric;
    }

    public double getValue() {
        return value;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
