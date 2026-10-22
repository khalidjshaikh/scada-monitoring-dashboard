package com.meridian.scada.web;

import com.meridian.scada.service.AlarmService;
import com.meridian.scada.web.dto.AcknowledgeResponse;
import com.meridian.scada.web.dto.AlarmView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @GetMapping
    public List<AlarmView> recent() {
        Instant now = Instant.now();
        return alarmService.recent().stream()
                .map(alarm -> ViewMapper.alarm(alarm, now))
                .toList();
    }

    @PostMapping("/{id}/acknowledge")
    public AlarmView acknowledge(@PathVariable Long id) {
        return ViewMapper.alarm(alarmService.acknowledge(id), Instant.now());
    }

    @PostMapping("/acknowledge-all")
    public AcknowledgeResponse acknowledgeAll() {
        int count = alarmService.acknowledgeAll();
        return new AcknowledgeResponse(count, count == 0
                ? "No unacknowledged alarms"
                : count + " alarm" + (count == 1 ? "" : "s") + " acknowledged");
    }
}
