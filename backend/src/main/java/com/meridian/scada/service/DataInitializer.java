package com.meridian.scada.service;

import com.meridian.scada.domain.Alarm;
import com.meridian.scada.domain.AlarmSeverity;
import com.meridian.scada.domain.AlarmStatus;
import com.meridian.scada.domain.Equipment;
import com.meridian.scada.domain.EquipmentMode;
import com.meridian.scada.domain.EquipmentState;
import com.meridian.scada.domain.EquipmentType;
import com.meridian.scada.domain.Metric;
import com.meridian.scada.domain.Site;
import com.meridian.scada.domain.Tank;
import com.meridian.scada.domain.TelemetryReading;
import com.meridian.scada.repository.AlarmRepository;
import com.meridian.scada.repository.EquipmentRepository;
import com.meridian.scada.repository.SiteRepository;
import com.meridian.scada.repository.TankRepository;
import com.meridian.scada.repository.TelemetryReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SiteRepository siteRepository;
    private final TankRepository tankRepository;
    private final EquipmentRepository equipmentRepository;
    private final AlarmRepository alarmRepository;
    private final TelemetryReadingRepository readingRepository;
    private final FleetState fleetState;
    private final SimulationEngine simulationEngine;

    public DataInitializer(SiteRepository siteRepository, TankRepository tankRepository,
                           EquipmentRepository equipmentRepository, AlarmRepository alarmRepository,
                           TelemetryReadingRepository readingRepository, FleetState fleetState,
                           SimulationEngine simulationEngine) {
        this.siteRepository = siteRepository;
        this.tankRepository = tankRepository;
        this.equipmentRepository = equipmentRepository;
        this.alarmRepository = alarmRepository;
        this.readingRepository = readingRepository;
        this.fleetState = fleetState;
        this.simulationEngine = simulationEngine;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (siteRepository.count() == 0) {
            seedFleet();
        }
        fleetState.load();
        simulationEngine.initialize();
    }

    private void seedFleet() {
        List<Site> sites = siteRepository.saveAll(List.of(
                new Site("WH-01", "North Wellhead", "Reeves County, TX", "Light sweet crude",
                        "16 in · West leg", 6, 62, 68.0, 52.0, 78.0, 380, 46.0, 68.0, 76.0, 1),
                new Site("CS-02", "Cedar Creek Compressor", "Ward County, TX", "Light sweet crude",
                        "16 in · Cedar Creek", 27, 44, 62.0, 46.0, 72.0, 450, 58.0, 72.0, 80.0, 2),
                new Site("PS-03", "Silverline Pump Station", "Pecos County, TX", "Light sweet crude",
                        "20 in · Central line", 49, 58, 54.0, 40.0, 64.0, 520, 51.0, 70.0, 78.0, 3),
                new Site("TF-04", "Red Mesa Tank Farm", "Pecos County, TX", "Light sweet crude",
                        "20 in · Storage leg", 71, 40, 22.0, 12.0, 32.0, 280, 34.0, 58.0, 66.0, 4),
                new Site("TM-05", "Gulf Terminal", "Matagorda County, TX", "Light sweet crude",
                        "24 in · Export leg", 93, 56, 16.0, 8.0, 26.0, 560, 31.0, 55.0, 63.0, 5)
        ));
        Map<String, Site> byCode = sites.stream()
                .collect(Collectors.toMap(Site::getCode, Function.identity()));

        Site wellhead = byCode.get("WH-01");
        Site compressor = byCode.get("CS-02");
        Site pump = byCode.get("PS-03");
        Site tankFarm = byCode.get("TF-04");
        Site terminal = byCode.get("TM-05");
        Instant now = Instant.now();

        tankRepository.saveAll(List.of(
                new Tank(tankFarm.getId(), tankFarm.getCode(), tankFarm.getName(), "T-401",
                        "Crude storage 401", "Light sweet crude", 180_000, 72.4),
                new Tank(tankFarm.getId(), tankFarm.getCode(), tankFarm.getName(), "T-402",
                        "Crude storage 402", "Light sweet crude", 140_000, 61.8),
                new Tank(tankFarm.getId(), tankFarm.getCode(), tankFarm.getName(), "T-403",
                        "Slop / interface tank", "Tank bottoms", 40_000, 16.9),
                new Tank(terminal.getId(), terminal.getCode(), terminal.getName(), "T-501",
                        "Export crude 501", "Light sweet crude", 250_000, 78.6),
                new Tank(terminal.getId(), terminal.getCode(), terminal.getName(), "T-502",
                        "Export blend 502", "Light sweet crude", 210_000, 54.2)
        ));

        equipmentRepository.saveAll(List.of(
                new Equipment(wellhead.getId(), wellhead.getCode(), wellhead.getName(), "P-101",
                        "Mainline transfer pump", EquipmentType.PUMP, EquipmentState.RUNNING,
                        EquipmentMode.REMOTE, true, "START", now.minus(3, ChronoUnit.HOURS)),
                new Equipment(wellhead.getId(), wellhead.getCode(), wellhead.getName(), "V-110",
                        "Wellhead isolation valve", EquipmentType.VALVE, EquipmentState.OPEN,
                        EquipmentMode.REMOTE, true, "OPEN", now.minus(14, ChronoUnit.HOURS)),
                new Equipment(compressor.getId(), compressor.getCode(), compressor.getName(), "C-201",
                        "Reciprocating compressor", EquipmentType.COMPRESSOR, EquipmentState.RUNNING,
                        EquipmentMode.REMOTE, true, "START", now.minus(6, ChronoUnit.HOURS)),
                new Equipment(compressor.getId(), compressor.getCode(), compressor.getName(), "V-210",
                        "Compressor recycle bypass", EquipmentType.VALVE, EquipmentState.CLOSED,
                        EquipmentMode.REMOTE, false, "CLOSE", now.minus(5, ChronoUnit.HOURS)),
                new Equipment(compressor.getId(), compressor.getCode(), compressor.getName(), "V-211",
                        "Discharge header isolation", EquipmentType.VALVE, EquipmentState.OPEN,
                        EquipmentMode.REMOTE, true, "OPEN", now.minus(9, ChronoUnit.HOURS)),
                new Equipment(pump.getId(), pump.getCode(), pump.getName(), "P-301",
                        "Booster pump A", EquipmentType.PUMP, EquipmentState.RUNNING,
                        EquipmentMode.REMOTE, true, "START", now.minus(2, ChronoUnit.HOURS)),
                new Equipment(pump.getId(), pump.getCode(), pump.getName(), "P-302",
                        "Booster pump B", EquipmentType.PUMP, EquipmentState.STANDBY,
                        EquipmentMode.REMOTE, true, "STANDBY", now.minus(27, ChronoUnit.HOURS)),
                new Equipment(pump.getId(), pump.getCode(), pump.getName(), "V-320",
                        "Station bypass header", EquipmentType.VALVE, EquipmentState.OPEN,
                        EquipmentMode.REMOTE, true, "OPEN", now.minus(18, ChronoUnit.HOURS)),
                new Equipment(tankFarm.getId(), tankFarm.getCode(), tankFarm.getName(), "V-410",
                        "Tank farm inlet", EquipmentType.VALVE, EquipmentState.OPEN,
                        EquipmentMode.REMOTE, true, "OPEN", now.minus(11, ChronoUnit.HOURS)),
                new Equipment(tankFarm.getId(), tankFarm.getCode(), tankFarm.getName(), "V-411",
                        "Export manifold", EquipmentType.VALVE, EquipmentState.OPEN,
                        EquipmentMode.REMOTE, true, "OPEN", now.minus(11, ChronoUnit.HOURS)),
                new Equipment(terminal.getId(), terminal.getCode(), terminal.getName(), "P-501",
                        "Marine export pump", EquipmentType.PUMP, EquipmentState.RUNNING,
                        EquipmentMode.REMOTE, true, "START", now.minus(4, ChronoUnit.HOURS)),
                new Equipment(terminal.getId(), terminal.getCode(), terminal.getName(), "V-510",
                        "Metering skid isolation", EquipmentType.VALVE, EquipmentState.OPEN,
                        EquipmentMode.REMOTE, true, "OPEN", now.minus(22, ChronoUnit.HOURS))
        ));

        Alarm critical = new Alarm(AlarmSeverity.CRITICAL, AlarmStatus.ACTIVE, "ALM-4471",
                "Redundant RTU heartbeat lost · primary link healthy", tankFarm.getId(),
                tankFarm.getCode(), tankFarm.getName(), "RTU-17", "ops:rtu-17:heartbeat",
                now.minus(6, ChronoUnit.MINUTES));
        Alarm warning = new Alarm(AlarmSeverity.WARNING, AlarmStatus.ACKNOWLEDGED, "ALM-4468",
                "C-201 discharge temperature trend high", compressor.getId(),
                compressor.getCode(), compressor.getName(), "C-201", "ops:c-201:trend",
                now.minus(22, ChronoUnit.MINUTES));
        warning.setAcknowledgedAt(now.minus(19, ChronoUnit.MINUTES));
        warning.setAcknowledgedBy("Shift A / console 2");
        Alarm info = new Alarm(AlarmSeverity.INFO, AlarmStatus.CLEARED, "ALM-4462",
                "Pig run PL-12 completed · Gulf Terminal received scraper", terminal.getId(),
                terminal.getCode(), terminal.getName(), "PL-12", "ops:pig-12:complete",
                now.minus(55, ChronoUnit.MINUTES));
        info.setClearedAt(now.minus(49, ChronoUnit.MINUTES));
        alarmRepository.saveAll(List.of(critical, warning, info));

        seedHistory(sites);
        log.info("Seeded {} stations, {} tanks, {} remote assets and {} alarms",
                sites.size(), 5, 12, 3);
    }

    private void seedHistory(List<Site> sites) {
        Instant end = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Random random = new Random(20261021);
        List<TelemetryReading> readings = new ArrayList<>(361 * sites.size() * 3);
        for (int step = 0; step <= 360; step++) {
            Instant timestamp = end.minusSeconds((360L - step) * 10);
            for (Site site : sites) {
                int order = site.getSortOrder();
                double pressure = site.getNominalPressure()
                        * (1 + 0.025 * Math.sin((step + order * 9) / 23.0)
                        + random.nextGaussian() * 0.006);
                if ("CS-02".equals(site.getCode()) && step >= 190 && step <= 202) {
                    pressure += 7.5 * Math.sin(Math.PI * (step - 190) / 12.0);
                }
                double flow = site.getNominalFlow()
                        * (1 + 0.04 * Math.sin((step + order * 17) / 19.0)
                        + random.nextGaussian() * 0.008);
                double temperature = site.getNominalTemperature()
                        * (1 + 0.018 * Math.sin((step + order * 13) / 31.0)
                        + random.nextGaussian() * 0.004);
                readings.add(new TelemetryReading(site.getId(), Metric.PRESSURE,
                        round(pressure, 2), timestamp));
                readings.add(new TelemetryReading(site.getId(), Metric.FLOW,
                        round(flow, 1), timestamp));
                readings.add(new TelemetryReading(site.getId(), Metric.TEMPERATURE,
                        round(temperature, 1), timestamp));
            }
        }
        readingRepository.saveAll(readings);
        log.info("Seeded {} telemetry readings (60 minute lookback)", readings.size());
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
