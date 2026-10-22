package com.meridian.scada.web.dto;

public record KpiView(
        double averagePressure,
        double totalFlow,
        double averageTemperature,
        double averageTankLevel,
        double totalInventoryBarrels,
        int activeAlarms,
        int criticalAlarms,
        int unacknowledgedAlarms,
        int equipmentTotal,
        int equipmentAvailable,
        double equipmentAvailabilityPercent
) {
}
