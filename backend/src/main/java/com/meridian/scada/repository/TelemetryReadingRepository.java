package com.meridian.scada.repository;

import com.meridian.scada.domain.Metric;
import com.meridian.scada.domain.TelemetryReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TelemetryReadingRepository extends JpaRepository<TelemetryReading, Long> {

    List<TelemetryReading> findByRecordedAtAfterOrderByRecordedAtAsc(Instant cutoff);

    List<TelemetryReading> findBySiteIdAndRecordedAtAfterOrderByRecordedAtAsc(Long siteId, Instant cutoff);

    Optional<TelemetryReading> findFirstBySiteIdAndMetricOrderByRecordedAtDesc(Long siteId, Metric metric);

    List<TelemetryReading> findTop48BySiteIdAndMetricOrderByRecordedAtDesc(Long siteId, Metric metric);

    long countByRecordedAtAfter(Instant cutoff);

    @Modifying
    @Transactional
    @Query("delete from TelemetryReading r where r.recordedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
