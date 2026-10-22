package com.meridian.scada.web.dto;

import com.meridian.scada.domain.OperationalStatus;

import java.util.List;

public record SiteView(
        Long id,
        String code,
        String name,
        String region,
        String commodity,
        String segment,
        double x,
        double y,
        double pressure,
        double flow,
        double temperature,
        double nominalFlow,
        double pressureLow,
        double pressureHigh,
        double temperatureWarning,
        double temperatureCritical,
        OperationalStatus status,
        OperationalStatus pressureStatus,
        OperationalStatus flowStatus,
        OperationalStatus temperatureStatus,
        int activeAlarms,
        int equipmentRunning,
        int equipmentTotal,
        List<Double> pressureTrend
) {
}
