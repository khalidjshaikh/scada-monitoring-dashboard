package com.meridian.scada.web.dto;

import com.meridian.scada.domain.TankLevelState;

public record TankView(
        Long id,
        Long siteId,
        String siteCode,
        String siteName,
        String tag,
        String name,
        String product,
        int capacityBarrels,
        double levelPercent,
        double volumeBarrels,
        TankLevelState levelState
) {
}
