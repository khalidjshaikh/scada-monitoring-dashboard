package com.meridian.scada.web.dto;

import java.time.Instant;

public record TrendPoint(
        Instant timestamp,
        double pressure,
        double flow,
        double temperature
) {
}
