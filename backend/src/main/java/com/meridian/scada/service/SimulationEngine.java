package com.meridian.scada.service;

import com.meridian.scada.domain.Equipment;
import com.meridian.scada.domain.EquipmentState;
import com.meridian.scada.domain.EquipmentType;
import com.meridian.scada.domain.Metric;
import com.meridian.scada.domain.Site;
import com.meridian.scada.domain.Tank;
import com.meridian.scada.domain.TelemetryReading;
import com.meridian.scada.repository.TelemetryReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Deterministic-ish pipeline simulation. Values are produced with first-order
 * lag around operating setpoints, correlated to the state of remote equipment.
 * A stopped pump or closed mainline valve visibly collapses throughput within a
 * few seconds, which makes the control loop demonstrable.
 */
@Service
public class SimulationEngine {

    private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);
    private static final int TREND_POINTS = 60;
    private static final int PERSIST_EVERY_TICKS = 5;
    private static final int FLUSH_EVERY_TICKS = 15;
    private static final int RETENTION_EVERY_TICKS = 120;

    private final FleetState fleetState;
    private final TelemetryReadingRepository readingRepository;
    private final AlarmService alarmService;
    private final Random random = new Random();
    private final Object lock = new Object();
    private final Map<Long, SiteRuntime> runtimes = new LinkedHashMap<>();
    private final Map<Long, List<Equipment>> equipmentBySite = new HashMap<>();

    private volatile boolean initialized;
    private Instant startedAt;
    private long tick;

    public SimulationEngine(FleetState fleetState, TelemetryReadingRepository readingRepository,
                            AlarmService alarmService) {
        this.fleetState = fleetState;
        this.readingRepository = readingRepository;
        this.alarmService = alarmService;
    }

    public void initialize() {
        synchronized (lock) {
            runtimes.clear();
            equipmentBySite.clear();
            for (Equipment item : fleetState.equipment()) {
                equipmentBySite.computeIfAbsent(item.getSiteId(), key -> new ArrayList<>()).add(item);
            }
            for (Site site : fleetState.sites()) {
                double pressure = latestValue(site.getId(), Metric.PRESSURE, site.getNominalPressure());
                double flow = latestValue(site.getId(), Metric.FLOW, site.getNominalFlow());
                double temperature = latestValue(site.getId(), Metric.TEMPERATURE, site.getNominalTemperature());
                Deque<Double> history = new ArrayDeque<>();
                List<TelemetryReading> seeded = readingRepository
                        .findTop48BySiteIdAndMetricOrderByRecordedAtDesc(site.getId(), Metric.PRESSURE);
                for (int i = seeded.size() - 1; i >= 0; i--) {
                    history.addLast(seeded.get(i).getValue());
                }
                if (history.isEmpty()) {
                    history.addLast(pressure);
                }
                runtimes.put(site.getId(), new SiteRuntime(site, pressure, flow, temperature, history));
            }
            startedAt = Instant.now();
            tick = 0;
            initialized = true;
            log.info("Simulation ready: {} stations, {} remote assets", runtimes.size(),
                    fleetState.equipment().size());
        }
    }

    @Scheduled(fixedRateString = "${scada.simulation.tick-ms:1000}", initialDelay = 1500)
    public void tick() {
        if (!initialized || !fleetState.isLoaded()) {
            return;
        }
        synchronized (lock) {
            tick++;
            long nowMillis = System.currentTimeMillis();
            double elapsed = (nowMillis - startedAt.toEpochMilli()) / 1000.0;

            for (SiteRuntime runtime : runtimes.values()) {
                advance(runtime, elapsed, nowMillis);
            }
            advanceTanks(elapsed);
            maybeInjectEvents(nowMillis);

            if (tick % 2 == 0) {
                Instant now = Instant.now();
                alarmService.evaluate(fleetState.sites(), telemetryValues(), fleetState.tanks(),
                        fleetState.equipment());
            }
            if (tick % PERSIST_EVERY_TICKS == 0) {
                persistReadings(Instant.now());
            }
            if (tick % FLUSH_EVERY_TICKS == 0) {
                fleetState.flushAssets();
            }
            if (tick % RETENTION_EVERY_TICKS == 0) {
                readingRepository.deleteOlderThan(Instant.now().minus(3, ChronoUnit.HOURS));
            }
        }
    }

    private void advance(SiteRuntime runtime, double elapsed, long nowMillis) {
        Site site = runtime.site;
        List<Equipment> siteEquipment = equipmentBySite.getOrDefault(site.getId(), List.of());

        boolean hasRotating = false;
        boolean rotatingRunning = false;
        boolean valvesOpen = true;
        for (Equipment item : siteEquipment) {
            if (!item.isFlowControl()) {
                continue;
            }
            if (item.getType() == EquipmentType.VALVE) {
                if (item.getState() != EquipmentState.OPEN) {
                    valvesOpen = false;
                }
            } else {
                hasRotating = true;
                if (item.getState() == EquipmentState.RUNNING) {
                    rotatingRunning = true;
                }
            }
        }
        if (!hasRotating) {
            rotatingRunning = true;
        }

        double availability = (rotatingRunning ? 1.0 : 0.08) * (valvesOpen ? 1.0 : 0.08);
        double pressureTarget = site.getNominalPressure() * (0.74 + 0.26 * availability)
                * (1.0 + 0.03 * Math.sin(elapsed / 47.0 + site.getSortOrder() * 0.9));
        if (nowMillis < runtime.pressureSpikeUntil) {
            pressureTarget += runtime.pressureSpikeAmplitude;
        }
        double flowTarget = site.getNominalFlow() * availability
                * (1.0 + 0.045 * Math.sin(elapsed / 29.0 + site.getSortOrder() * 1.3));
        double temperatureTarget = site.getNominalTemperature()
                * (1.0 + 0.018 * Math.sin(elapsed / 73.0 + site.getSortOrder()))
                + (rotatingRunning ? 1.4 : -1.2);
        if (nowMillis < runtime.temperatureSpikeUntil) {
            temperatureTarget += runtime.temperatureSpikeAmplitude;
        }

        runtime.pressure += (pressureTarget - runtime.pressure) * 0.065 + random.nextGaussian() * 0.22;
        runtime.flow += (flowTarget - runtime.flow) * 0.10 + random.nextGaussian() * 1.2;
        runtime.temperature += (temperatureTarget - runtime.temperature) * 0.035
                + random.nextGaussian() * 0.10;

        runtime.pressure = clamp(runtime.pressure, 0.5, site.getPressureHigh() + 30);
        runtime.flow = clamp(runtime.flow, 0, site.getNominalFlow() * 1.4);
        runtime.temperature = clamp(runtime.temperature, -10, site.getTemperatureCritical() + 25);

        runtime.pressureTrend.addLast(round(runtime.pressure, 2));
        while (runtime.pressureTrend.size() > TREND_POINTS) {
            runtime.pressureTrend.removeFirst();
        }
    }

    private void advanceTanks(double elapsed) {
        for (Tank tank : fleetState.tanks()) {
            double phase = (tank.getId() == null ? 0 : tank.getId()) * 2.31 + elapsed / 140.0;
            double wave = Math.sin(phase) * 0.010;
            double noise = random.nextGaussian() * 0.006;
            double direction = tank.getSiteCode().startsWith("TF") ? 0.0012 : -0.0009;
            double level = tank.getLevelPercent() + wave + noise + direction;
            tank.setLevelPercent(round(clamp(level, 2.0, 98.5), 2));
        }
    }

    private void maybeInjectEvents(long nowMillis) {
        for (SiteRuntime runtime : runtimes.values()) {
            if (nowMillis > runtime.pressureSpikeUntil && random.nextDouble() < 0.0007) {
                runtime.pressureSpikeUntil = nowMillis + 11_000 + random.nextInt(9_000);
                runtime.pressureSpikeAmplitude = 10 + random.nextDouble() * 13;
            }
            if (nowMillis > runtime.temperatureSpikeUntil && random.nextDouble() < 0.00025) {
                runtime.temperatureSpikeUntil = nowMillis + 14_000 + random.nextInt(10_000);
                runtime.temperatureSpikeAmplitude = 12 + random.nextDouble() * 11;
            }
        }
        for (Equipment item : fleetState.equipment()) {
            if (item.getState() != EquipmentState.FAULT && random.nextDouble() < 0.00008) {
                item.setState(EquipmentState.FAULT);
                item.setLastCommand("TRIP");
                item.setLastCommandAt(Instant.now());
                fleetState.saveEquipment(item);
                log.warn("Simulated trip on {} ({})", item.getTag(), item.getName());
            }
        }
    }

    private void persistReadings(Instant now) {
        List<TelemetryReading> batch = new ArrayList<>(runtimes.size() * 3);
        for (SiteRuntime runtime : runtimes.values()) {
            batch.add(new TelemetryReading(runtime.site.getId(), Metric.PRESSURE,
                    round(runtime.pressure, 2), now));
            batch.add(new TelemetryReading(runtime.site.getId(), Metric.FLOW,
                    round(runtime.flow, 1), now));
            batch.add(new TelemetryReading(runtime.site.getId(), Metric.TEMPERATURE,
                    round(runtime.temperature, 1), now));
        }
        readingRepository.saveAll(batch);
    }

    private Map<Long, TelemetryValue> telemetryValues() {
        Map<Long, TelemetryValue> values = new HashMap<>();
        runtimes.forEach((id, runtime) -> values.put(id,
                new TelemetryValue(runtime.pressure, runtime.flow, runtime.temperature)));
        return values;
    }

    public Map<Long, SiteRuntimeSnapshot> snapshot() {
        synchronized (lock) {
            Map<Long, SiteRuntimeSnapshot> copy = new LinkedHashMap<>();
            runtimes.forEach((id, runtime) -> copy.put(id, new SiteRuntimeSnapshot(
                    round(runtime.pressure, 2),
                    round(runtime.flow, 1),
                    round(runtime.temperature, 1),
                    List.copyOf(runtime.pressureTrend))));
            return copy;
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    private double latestValue(Long siteId, Metric metric, double fallback) {
        return readingRepository.findFirstBySiteIdAndMetricOrderByRecordedAtDesc(siteId, metric)
                .map(TelemetryReading::getValue)
                .orElse(fallback);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    public record SiteRuntimeSnapshot(
            double pressure,
            double flow,
            double temperature,
            List<Double> pressureTrend
    ) {
    }

    private static final class SiteRuntime {
        private final Site site;
        private final Deque<Double> pressureTrend;
        private double pressure;
        private double flow;
        private double temperature;
        private long pressureSpikeUntil;
        private double pressureSpikeAmplitude;
        private long temperatureSpikeUntil;
        private double temperatureSpikeAmplitude;

        private SiteRuntime(Site site, double pressure, double flow, double temperature,
                            Deque<Double> pressureTrend) {
            this.site = site;
            this.pressure = pressure;
            this.flow = flow;
            this.temperature = temperature;
            this.pressureTrend = pressureTrend;
        }
    }
}
