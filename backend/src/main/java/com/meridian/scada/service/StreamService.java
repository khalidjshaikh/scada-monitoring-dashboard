package com.meridian.scada.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class StreamService {

    private static final Logger log = LoggerFactory.getLogger(StreamService.class);

    private final DashboardService dashboardService;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public StreamService(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event()
                    .name("snapshot")
                    .data(dashboardService.snapshot(), MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException exception) {
            emitters.remove(emitter);
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    @Scheduled(fixedRateString = "${scada.stream.broadcast-ms:2000}", initialDelay = 2000)
    public void broadcast() {
        if (emitters.isEmpty()) {
            return;
        }
        Object snapshot = dashboardService.snapshot();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("snapshot")
                        .data(snapshot, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException exception) {
                emitters.remove(emitter);
                emitter.complete();
            }
        }
    }
}
