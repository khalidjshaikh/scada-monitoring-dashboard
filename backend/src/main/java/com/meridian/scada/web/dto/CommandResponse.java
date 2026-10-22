package com.meridian.scada.web.dto;

public record CommandResponse(
        boolean accepted,
        String message,
        EquipmentView equipment
) {
}
