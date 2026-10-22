package com.meridian.scada.web.dto;

import com.meridian.scada.domain.EquipmentMode;
import com.meridian.scada.domain.EquipmentState;
import com.meridian.scada.domain.EquipmentType;

import java.time.Instant;
import java.util.List;

public record EquipmentView(
        Long id,
        Long siteId,
        String siteCode,
        String siteName,
        String tag,
        String name,
        EquipmentType type,
        EquipmentState state,
        EquipmentMode mode,
        boolean flowControl,
        String lastCommand,
        Instant lastCommandAt,
        List<String> actions
) {
}
