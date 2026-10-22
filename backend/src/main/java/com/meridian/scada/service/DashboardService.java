package com.meridian.scada.service;

import com.meridian.scada.domain.Alarm;
import com.meridian.scada.domain.AlarmSeverity;
import com.meridian.scada.domain.AlarmStatus;
import com.meridian.scada.domain.Equipment;
import com.meridian.scada.domain.EquipmentState;
import com.meridian.scada.domain.OperationalStatus;
import com.meridian.scada.domain.Site;
import com.meridian.scada.domain.Tank;
import com.meridian.scada.web.ViewMapper;
import com.meridian.scada.web.dto.AlarmView;
import com.meridian.scada.web.dto.DashboardSnapshot;
import com.meridian.scada.web.dto.EquipmentView;
import com.meridian.scada.web.dto.KpiView;
import com.meridian.scada.web.dto.SiteView;
import com.meridian.scada.web.dto.TankView;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final FleetState fleetState;
    private final SimulationEngine simulationEngine;
    private final AlarmService alarmService;
    private final SystemInfoService systemInfoService;

    public DashboardService(FleetState fleetState, SimulationEngine simulationEngine,
                            AlarmService alarmService, SystemInfoService systemInfoService) {
        this.fleetState = fleetState;
        this.simulationEngine = simulationEngine;
        this.alarmService = alarmService;
        this.systemInfoService = systemInfoService;
    }

    public DashboardSnapshot snapshot() {
        Instant now = Instant.now();
        List<Site> sites = fleetState.sites();
        List<Tank> tanks = fleetState.tanks();
        List<Equipment> equipment = fleetState.equipment();
        List<Alarm> activeAlarms = alarmService.active();
        List<Alarm> recentAlarms = alarmService.recent();
        Map<Long, SimulationEngine.SiteRuntimeSnapshot> telemetry = simulationEngine.snapshot();

        Map<Long, List<Alarm>> alarmsBySite = new HashMap<>();
        for (Alarm alarm : activeAlarms) {
            if (alarm.getSiteId() != null) {
                alarmsBySite.computeIfAbsent(alarm.getSiteId(), key -> new ArrayList<>()).add(alarm);
            }
        }
        Map<Long, List<Equipment>> equipmentBySite = new HashMap<>();
        for (Equipment item : equipment) {
            equipmentBySite.computeIfAbsent(item.getSiteId(), key -> new ArrayList<>()).add(item);
        }

        List<SiteView> siteViews = new ArrayList<>();
        double pressureSum = 0;
        double flowSum = 0;
        double temperatureSum = 0;
        for (Site site : sites) {
            SimulationEngine.SiteRuntimeSnapshot runtime = telemetry.get(site.getId());
            double pressure = runtime == null ? site.getNominalPressure() : runtime.pressure();
            double flow = runtime == null ? site.getNominalFlow() : runtime.flow();
            double temperature = runtime == null
                    ? site.getNominalTemperature() : runtime.temperature();
            List<Alarm> siteAlarms = alarmsBySite.getOrDefault(site.getId(), List.of());
            List<Equipment> siteEquipment = equipmentBySite.getOrDefault(site.getId(), List.of());
            OperationalStatus pressureStatus = pressureStatus(site, pressure);
            OperationalStatus flowStatus = flowStatus(site, flow);
            OperationalStatus temperatureStatus = temperatureStatus(site, temperature);
            OperationalStatus status = worst(worst(pressureStatus, flowStatus), temperatureStatus);
            for (Alarm alarm : siteAlarms) {
                status = worst(status, alarm.getSeverity() == AlarmSeverity.CRITICAL
                        ? OperationalStatus.CRITICAL : OperationalStatus.WARNING);
            }
            int equipmentRunning = (int) siteEquipment.stream()
                    .filter(item -> item.getState() == EquipmentState.RUNNING
                            || item.getState() == EquipmentState.OPEN)
                    .count();
            List<Double> pressureTrend = runtime == null || runtime.pressureTrend().isEmpty()
                    ? List.of(round(pressure, 2))
                    : runtime.pressureTrend();

            siteViews.add(new SiteView(
                    site.getId(),
                    site.getCode(),
                    site.getName(),
                    site.getRegion(),
                    site.getCommodity(),
                    site.getSegment(),
                    site.getX(),
                    site.getY(),
                    round(pressure, 2),
                    round(flow, 1),
                    round(temperature, 1),
                    site.getNominalFlow(),
                    site.getPressureLow(),
                    site.getPressureHigh(),
                    site.getTemperatureWarning(),
                    site.getTemperatureCritical(),
                    status,
                    pressureStatus,
                    flowStatus,
                    temperatureStatus,
                    siteAlarms.size(),
                    equipmentRunning,
                    siteEquipment.size(),
                    pressureTrend));

            pressureSum += pressure;
            flowSum += flow;
            temperatureSum += temperature;
        }
        siteViews.sort(Comparator.comparing(SiteView::code));

        List<TankView> tankViews = tanks.stream().map(ViewMapper::tank).toList();
        List<EquipmentView> equipmentViews = equipment.stream().map(ViewMapper::equipment).toList();
        List<AlarmView> alarmViews = recentAlarms.stream()
                .map(alarm -> ViewMapper.alarm(alarm, now))
                .toList();

        long totalCapacity = 0;
        double totalVolume = 0;
        for (Tank tank : tanks) {
            totalCapacity += tank.getCapacityBarrels();
            totalVolume += tank.getCapacityBarrels() * tank.getLevelPercent() / 100.0;
        }
        double averageTankLevel = totalCapacity == 0 ? 0 : totalVolume / totalCapacity * 100.0;
        int activeCount = activeAlarms.size();
        int criticalCount = (int) activeAlarms.stream()
                .filter(alarm -> alarm.getSeverity() == AlarmSeverity.CRITICAL)
                .count();
        int unacknowledged = (int) activeAlarms.stream()
                .filter(alarm -> alarm.getStatus() == AlarmStatus.ACTIVE)
                .count();
        int equipmentAvailable = (int) equipment.stream()
                .filter(item -> item.getState() != EquipmentState.FAULT)
                .count();

        KpiView kpis = new KpiView(
                sites.isEmpty() ? 0 : round(pressureSum / sites.size(), 2),
                round(flowSum, 0),
                sites.isEmpty() ? 0 : round(temperatureSum / sites.size(), 1),
                round(averageTankLevel, 1),
                Math.round(totalVolume),
                activeCount,
                criticalCount,
                unacknowledged,
                equipment.size(),
                equipmentAvailable,
                equipment.isEmpty() ? 0 : round(equipmentAvailable * 100.0 / equipment.size(), 1));

        return new DashboardSnapshot(now, kpis, siteViews, tankViews, equipmentViews,
                alarmViews, systemInfoService.info());
    }

    private static OperationalStatus pressureStatus(Site site, double pressure) {
        if (pressure > site.getPressureHigh() + 8 || pressure < site.getPressureLow() - 8) {
            return OperationalStatus.CRITICAL;
        }
        if (pressure > site.getPressureHigh() || pressure < site.getPressureLow()) {
            return OperationalStatus.WARNING;
        }
        return OperationalStatus.NORMAL;
    }

    private static OperationalStatus flowStatus(Site site, double flow) {
        if (site.getNominalFlow() <= 0) {
            return OperationalStatus.NORMAL;
        }
        double ratio = flow / site.getNominalFlow();
        if (ratio < 0.15) {
            return OperationalStatus.CRITICAL;
        }
        if (ratio < 0.35) {
            return OperationalStatus.WARNING;
        }
        return OperationalStatus.NORMAL;
    }

    private static OperationalStatus temperatureStatus(Site site, double temperature) {
        if (temperature > site.getTemperatureCritical()) {
            return OperationalStatus.CRITICAL;
        }
        if (temperature > site.getTemperatureWarning()) {
            return OperationalStatus.WARNING;
        }
        return OperationalStatus.NORMAL;
    }

    private static OperationalStatus worst(OperationalStatus left, OperationalStatus right) {
        return severity(left) >= severity(right) ? left : right;
    }

    private static int severity(OperationalStatus status) {
        return switch (status) {
            case NORMAL -> 0;
            case WARNING -> 1;
            case CRITICAL -> 2;
            case OFFLINE -> 3;
        };
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
