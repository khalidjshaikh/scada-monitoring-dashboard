package com.meridian.scada.service;

import com.meridian.scada.repository.TelemetryReadingRepository;
import com.meridian.scada.web.dto.SystemView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;

@Service
public class SystemInfoService {

    private final Instant startedAt = Instant.now();
    private final TelemetryReadingRepository readingRepository;
    private final long tickMs;
    private final String database;
    private volatile long cachedRows;
    private volatile Instant cachedAt = Instant.EPOCH;

    public SystemInfoService(TelemetryReadingRepository readingRepository, DataSource dataSource,
                             @Value("${scada.simulation.tick-ms:1000}") long tickMs) {
        this.readingRepository = readingRepository;
        this.tickMs = tickMs;
        this.database = resolveDatabase(dataSource);
    }

    public SystemView info() {
        Instant now = Instant.now();
        if (Duration.between(cachedAt, now).toSeconds() >= 5) {
            cachedRows = readingRepository.count();
            cachedAt = now;
        }
        return new SystemView(
                Duration.between(startedAt, now).toSeconds(),
                cachedRows,
                database,
                "Java " + Runtime.version().feature(),
                (int) tickMs);
    }

    private static String resolveDatabase(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            var metadata = connection.getMetaData();
            return metadata.getDatabaseProductName() + " " + metadata.getDatabaseProductVersion();
        } catch (Exception ignored) {
            return "H2";
        }
    }
}
