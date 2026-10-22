package com.meridian.scada.service;

import com.meridian.scada.domain.Alarm;
import com.meridian.scada.domain.AlarmSeverity;
import com.meridian.scada.domain.AlarmStatus;
import com.meridian.scada.domain.Equipment;
import com.meridian.scada.domain.EquipmentState;
import com.meridian.scada.domain.Site;
import com.meridian.scada.domain.Tank;
import com.meridian.scada.repository.AlarmRepository;
import com.meridian.scada.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final AtomicInteger codeSequence = new AtomicInteger(5000);
    private final Map<String, Instant> lowFlowSince = new ConcurrentHashMap<>();

    public AlarmService(AlarmRepository alarmRepository) {
        this.alarmRepository = alarmRepository;
    }

    public List<Alarm> recent() {
        return alarmRepository.findTop60ByOrderByRaisedAtDesc();
    }

    public List<Alarm> active() {
        return alarmRepository.findByStatusNotOrderByRaisedAtDesc(AlarmStatus.CLEARED);
    }

    public List<Alarm> cleared() {
        return alarmRepository.findByStatusOrderByRaisedAtDesc(AlarmStatus.CLEARED);
    }

    @Transactional
    public void evaluate(List<Site> sites, Map<Long, TelemetryValue> telemetry,
                         List<Tank> tanks, List<Equipment> equipment) {
        Instant now = Instant.now();
        Map<String, Alarm> active = new HashMap<>();
        for (Alarm alarm : alarmRepository.findByStatusNotOrderByRaisedAtDesc(AlarmStatus.CLEARED)) {
            active.put(alarm.getRuleKey(), alarm);
        }
        Map<Long, Site> siteById = new LinkedHashMap<>();
        sites.forEach(site -> siteById.put(site.getId(), site));
        List<Alarm> dirty = new ArrayList<>();

        for (Site site : sites) {
            TelemetryValue value = telemetry.get(site.getId());
            if (value == null) {
                continue;
            }
            evaluatePressure(site, value.pressure(), active, dirty, now);
            evaluateTemperature(site, value.temperature(), active, dirty, now);
            evaluateFlow(site, value.flow(), active, dirty, now);
        }

        for (Tank tank : tanks) {
            Site site = siteById.get(tank.getSiteId());
            evaluateTank(tank, site, active, dirty, now);
        }

        for (Equipment item : equipment) {
            Site site = siteById.get(item.getSiteId());
            String ruleKey = "equipment:" + item.getTag() + ":fault";
            boolean faulted = item.getState() == EquipmentState.FAULT;
            sync(active, dirty, now, ruleKey, faulted, AlarmSeverity.CRITICAL,
                    item.getTag() + " is in fault state",
                    site, item.getSiteName(), item.getTag());
        }

        if (!dirty.isEmpty()) {
            alarmRepository.saveAll(dirty);
        }
    }

    private void evaluatePressure(Site site, double pressure, Map<String, Alarm> active,
                                  List<Alarm> dirty, Instant now) {
        String highKey = "site:" + site.getCode() + ":pressure-high";
        AlarmSeverity highSeverity = pressure >= site.getPressureHigh() + 8
                ? AlarmSeverity.CRITICAL : AlarmSeverity.WARNING;
        sync(active, dirty, now, highKey, pressure > site.getPressureHigh(), highSeverity,
                "Discharge pressure high: %.1f bar (limit %.1f bar)"
                        .formatted(pressure, site.getPressureHigh()),
                site, site.getCode(), "PT-" + site.getCode());

        String lowKey = "site:" + site.getCode() + ":pressure-low";
        AlarmSeverity lowSeverity = pressure <= site.getPressureLow() - 8
                ? AlarmSeverity.CRITICAL : AlarmSeverity.WARNING;
        sync(active, dirty, now, lowKey, pressure < site.getPressureLow(), lowSeverity,
                "Discharge pressure low: %.1f bar (limit %.1f bar)"
                        .formatted(pressure, site.getPressureLow()),
                site, site.getCode(), "PT-" + site.getCode());
    }

    private void evaluateTemperature(Site site, double temperature, Map<String, Alarm> active,
                                     List<Alarm> dirty, Instant now) {
        String key = "site:" + site.getCode() + ":temperature-high";
        AlarmSeverity severity = temperature >= site.getTemperatureCritical()
                ? AlarmSeverity.CRITICAL : AlarmSeverity.WARNING;
        sync(active, dirty, now, key, temperature > site.getTemperatureWarning(), severity,
                "Process temperature high: %.1f °C (limit %.1f °C)"
                        .formatted(temperature, site.getTemperatureWarning()),
                site, site.getCode(), "TT-" + site.getCode());
    }

    private void evaluateFlow(Site site, double flow, Map<String, Alarm> active,
                              List<Alarm> dirty, Instant now) {
        String key = "site:" + site.getCode() + ":flow-low";
        double ratio = site.getNominalFlow() <= 0 ? 1 : flow / site.getNominalFlow();
        if (ratio >= 0.35) {
            lowFlowSince.remove(key);
            sync(active, dirty, now, key, false, AlarmSeverity.WARNING,
                    "Line flow below minimum", site, site.getCode(), "FT-" + site.getCode());
            return;
        }

        Instant since = lowFlowSince.computeIfAbsent(key, ignored -> now);
        if (Duration.between(since, now).toSeconds() < 3) {
            return;
        }
        AlarmSeverity severity = ratio < 0.15 ? AlarmSeverity.CRITICAL : AlarmSeverity.WARNING;
        sync(active, dirty, now, key, true, severity,
                "Line flow below minimum: %.0f m³/h (minimum %.0f m³/h)"
                        .formatted(flow, site.getNominalFlow() * 0.35),
                site, site.getCode(), "FT-" + site.getCode());
    }

    private void evaluateTank(Tank tank, Site site, Map<String, Alarm> active,
                              List<Alarm> dirty, Instant now) {
        String lowKey = "tank:" + tank.getTag() + ":low";
        AlarmSeverity lowSeverity = tank.getLevelPercent() < 8
                ? AlarmSeverity.CRITICAL : AlarmSeverity.WARNING;
        sync(active, dirty, now, lowKey, tank.getLevelPercent() < 15, lowSeverity,
                "%s level low: %.1f%% (reserve %.0f%%)"
                        .formatted(tank.getTag(), tank.getLevelPercent(), 15.0),
                site, tank.getTag(), tank.getTag() + " / LIT");

        String highKey = "tank:" + tank.getTag() + ":high";
        AlarmSeverity highSeverity = tank.getLevelPercent() > 97
                ? AlarmSeverity.CRITICAL : AlarmSeverity.WARNING;
        sync(active, dirty, now, highKey, tank.getLevelPercent() > 92, highSeverity,
                "%s level high: %.1f%% (limit %.0f%%)"
                        .formatted(tank.getTag(), tank.getLevelPercent(), 92.0),
                site, tank.getTag(), tank.getTag() + " / LIT");
    }

    private void sync(Map<String, Alarm> active, List<Alarm> dirty, Instant now,
                      String ruleKey, boolean condition, AlarmSeverity severity,
                      String message, Site site, String source, String sourceTag) {
        Alarm existing = active.get(ruleKey);
        if (condition) {
            if (existing == null) {
                String code = "ALM-" + codeSequence.incrementAndGet();
                Alarm alarm = new Alarm(severity, AlarmStatus.ACTIVE, code, message,
                        site == null ? null : site.getId(),
                        site == null ? null : site.getCode(),
                        site == null ? null : site.getName(),
                        source, ruleKey, now);
                active.put(ruleKey, alarm);
                dirty.add(alarm);
            } else if (severity.ordinal() < existing.getSeverity().ordinal()) {
                existing.setSeverity(severity);
                existing.setMessage(message);
                existing.setStatus(AlarmStatus.ACTIVE);
                existing.setAcknowledgedAt(null);
                existing.setAcknowledgedBy(null);
                dirty.add(existing);
            }
        } else if (existing != null) {
            existing.setStatus(AlarmStatus.CLEARED);
            existing.setClearedAt(now);
            active.remove(ruleKey);
            dirty.add(existing);
        }
    }

    @Transactional
    public Alarm acknowledge(Long id) {
        Alarm alarm = alarmRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alarm not found"));
        if (alarm.getStatus() == AlarmStatus.CLEARED) {
            throw new ApiException(HttpStatus.CONFLICT, "Cleared alarms cannot be acknowledged");
        }
        if (alarm.getStatus() == AlarmStatus.ACKNOWLEDGED) {
            return alarm;
        }
        alarm.setStatus(AlarmStatus.ACKNOWLEDGED);
        alarm.setAcknowledgedAt(Instant.now());
        alarm.setAcknowledgedBy("Shift A / console 2");
        return alarmRepository.save(alarm);
    }

    @Transactional
    public int acknowledgeAll() {
        List<Alarm> pending = alarmRepository.findByStatusOrderByRaisedAtDesc(AlarmStatus.ACTIVE);
        Instant now = Instant.now();
        for (Alarm alarm : pending) {
            alarm.setStatus(AlarmStatus.ACKNOWLEDGED);
            alarm.setAcknowledgedAt(now);
            alarm.setAcknowledgedBy("Shift A / console 2");
        }
        alarmRepository.saveAll(pending);
        return pending.size();
    }

    @Transactional
    public void clearRule(String ruleKey) {
        Optional<Alarm> alarm = alarmRepository.findFirstByRuleKeyAndStatusNotOrderByRaisedAtDesc(
                ruleKey, AlarmStatus.CLEARED);
        alarm.ifPresent(existing -> {
            existing.setStatus(AlarmStatus.CLEARED);
            existing.setClearedAt(Instant.now());
            alarmRepository.save(existing);
        });
    }
}
