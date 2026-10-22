package com.meridian.scada.web;

import com.meridian.scada.service.DashboardService;
import com.meridian.scada.service.TelemetryHistoryService;
import com.meridian.scada.web.dto.DashboardSnapshot;
import com.meridian.scada.web.dto.TrendResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;
    private final TelemetryHistoryService historyService;

    public DashboardController(DashboardService dashboardService,
                               TelemetryHistoryService historyService) {
        this.dashboardService = dashboardService;
        this.historyService = historyService;
    }

    @GetMapping("/dashboard")
    public DashboardSnapshot dashboard() {
        return dashboardService.snapshot();
    }

    @GetMapping("/trends")
    public TrendResponse trends(@RequestParam(defaultValue = "15") int minutes,
                                @RequestParam(required = false) Long siteId) {
        return historyService.trend(minutes, siteId);
    }
}
