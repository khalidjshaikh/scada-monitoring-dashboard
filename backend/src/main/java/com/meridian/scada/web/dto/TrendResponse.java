package com.meridian.scada.web.dto;

import java.time.Instant;
import java.util.List;

public record TrendResponse(
        int minutes,
        Long siteId,
        List<TrendPoint> points,
        double targetMin,
        double targetMax,
        Instant generatedAt
) {
}
