package com.meridian.scada.web.dto;

import java.time.Instant;
import java.util.List;

public record DashboardSnapshot(
        Instant timestamp,
        KpiView kpis,
        List<SiteView> sites,
        List<TankView> tanks,
        List<EquipmentView> equipment,
        List<AlarmView> alarms,
        SystemView system
) {
}
