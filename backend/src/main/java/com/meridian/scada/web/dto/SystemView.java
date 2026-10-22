package com.meridian.scada.web.dto;

public record SystemView(
        long uptimeSeconds,
        long readingsStored,
        String database,
        String runtime,
        int tickMs
) {
}
