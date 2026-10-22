package com.meridian.scada.web.dto;

import com.meridian.scada.domain.AlarmSeverity;
import com.meridian.scada.domain.AlarmStatus;

import java.time.Instant;

public record AlarmView(
        Long id,
        String code,
        AlarmSeverity severity,
        AlarmStatus status,
        String message,
        Long siteId,
        String siteCode,
        String siteName,
        String source,
        Instant raisedAt,
        Instant acknowledgedAt,
        String acknowledgedBy,
        Instant clearedAt,
        long ageSeconds
) {
}
