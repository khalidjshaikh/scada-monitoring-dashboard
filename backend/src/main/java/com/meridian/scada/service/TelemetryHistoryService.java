package com.meridian.scada.service;

import com.meridian.scada.domain.Metric;
import com.meridian.scada.domain.Site;
import com.meridian.scada.domain.TelemetryReading;
import com.meridian.scada.repository.SiteRepository;
import com.meridian.scada.repository.TelemetryReadingRepository;
import com.meridian.scada.web.ApiException;
import com.meridian.scada.web.dto.TrendPoint;
import com.meridian.scada.web.dto.TrendResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class TelemetryHistoryService {

    private static final int MAX_POINTS = 420;

    private final TelemetryReadingRepository readingRepository;
    private final SiteRepository siteRepository;

    public TelemetryHistoryService(TelemetryReadingRepository readingRepository,
                                   SiteRepository siteRepository) {
        this.readingRepository = readingRepository;
        this.siteRepository = siteRepository;
    }

    public TrendResponse trend(int requestedMinutes, Long siteId) {
        int minutes = Math.max(1, Math.min(180, requestedMinutes));
        Instant cutoff = Instant.now().minus(minutes, ChronoUnit.MINUTES);
        List<TelemetryReading> rows = siteId == null
                ? readingRepository.findByRecordedAtAfterOrderByRecordedAtAsc(cutoff)
                : readingRepository.findBySiteIdAndRecordedAtAfterOrderByRecordedAtAsc(siteId, cutoff);

        TreeMap<Instant, Bucket> buckets = new TreeMap<>();
        for (TelemetryReading reading : rows) {
            Instant key = reading.getRecordedAt().truncatedTo(ChronoUnit.SECONDS);
            Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket());
            bucket.add(reading.getMetric(), reading.getValue());
        }

        List<TrendPoint> points = new ArrayList<>(buckets.size());
        buckets.forEach((timestamp, bucket) -> {
            points.add(new TrendPoint(
                    timestamp,
                    round(bucket.pressure(), 2),
                    round(bucket.flow(), 1),
                    round(bucket.temperature(), 1)));
        });
        List<TrendPoint> sampled = downsample(points);

        double targetMin;
        double targetMax;
        List<Site> sites = siteRepository.findAllByOrderBySortOrderAsc();
        if (siteId != null) {
            Site site = siteRepository.findById(siteId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Station not found"));
            targetMin = site.getPressureLow();
            targetMax = site.getPressureHigh();
        } else if (sites.isEmpty()) {
            targetMin = 0;
            targetMax = 0;
        } else {
            targetMin = sites.stream().mapToDouble(Site::getPressureLow).average().orElse(0);
            targetMax = sites.stream().mapToDouble(Site::getPressureHigh).average().orElse(0);
        }

        return new TrendResponse(minutes, siteId, sampled,
                round(targetMin, 1), round(targetMax, 1), Instant.now());
    }

    private static List<TrendPoint> downsample(List<TrendPoint> points) {
        if (points.size() <= MAX_POINTS) {
            return points;
        }
        int step = (int) Math.ceil(points.size() / (double) MAX_POINTS);
        List<TrendPoint> sampled = new ArrayList<>(MAX_POINTS);
        for (int i = 0; i < points.size(); i += step) {
            sampled.add(points.get(i));
        }
        TrendPoint last = points.getLast();
        if (!sampled.getLast().equals(last)) {
            sampled.add(last);
        }
        return sampled;
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    private static final class Bucket {
        private double pressure;
        private double flow;
        private double temperature;
        private int pressureCount;
        private int flowCount;
        private int temperatureCount;

        void add(Metric metric, double value) {
            switch (metric) {
                case PRESSURE -> {
                    pressure += value;
                    pressureCount++;
                }
                case FLOW -> {
                    flow += value;
                    flowCount++;
                }
                case TEMPERATURE -> {
                    temperature += value;
                    temperatureCount++;
                }
            }
        }

        double pressure() {
            return pressureCount == 0 ? 0 : pressure / pressureCount;
        }

        double flow() {
            // Flow is additive across stations; for a single station the aggregate is
            // that station's throughput.
            return flow;
        }

        double temperature() {
            return temperatureCount == 0 ? 0 : temperature / temperatureCount;
        }
    }
}
