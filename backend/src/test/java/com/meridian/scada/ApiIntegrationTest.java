package com.meridian.scada;

import com.meridian.scada.domain.Alarm;
import com.meridian.scada.repository.AlarmRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the real application context (H2, seeded fleet, simulation and HTTP layer)
 * and exercises the operator-facing API contracts.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AlarmRepository alarmRepository;

    @Test
    void dashboardExposesTheSeededOperatingPicture() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.kpis").exists())
                .andExpect(jsonPath("$.kpis.activeAlarms").isNumber())
                .andExpect(jsonPath("$.sites").isArray())
                .andExpect(jsonPath("$.sites.length()").value(5))
                .andExpect(jsonPath("$.tanks.length()").value(5))
                .andExpect(jsonPath("$.equipment.length()").value(12))
                .andExpect(jsonPath("$.system").exists());
    }

    @Test
    void trendHistoryIsReturnedForTheRequestedWindow() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/trends").param("minutes", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minutes").value(15))
                .andExpect(jsonPath("$.points").isArray())
                .andExpect(jsonPath("$.generatedAt").isString())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"pressure\"", "\"flow\"", "\"temperature\"");
    }

    @Test
    void alarmAcknowledgementIsIdempotentAndPersisted() throws Exception {
        Alarm active = alarmRepository.findByStatusNotOrderByRaisedAtDesc(
                        com.meridian.scada.domain.AlarmStatus.CLEARED)
                .stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected the demo dataset to raise an alarm"));

        MvcResult first = mockMvc.perform(post("/api/alarms/{id}/acknowledge", active.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(active.getId()))
                .andExpect(jsonPath("$.acknowledgedAt").isString())
                .andReturn();

        String acknowledgedAt = first.getResponse().getContentAsString();
        mockMvc.perform(post("/api/alarms/{id}/acknowledge", active.getId()))
                .andExpect(status().isOk());

        Alarm refreshed = alarmRepository.findById(active.getId()).orElseThrow();
        assertThat(refreshed.getAcknowledgedAt()).isNotNull();
        assertThat(refreshed.getAcknowledgedAt()).isBefore(Instant.now().plusSeconds(1));
        assertThat(acknowledgedAt).contains("\"acknowledgedBy\"");

        mockMvc.perform(post("/api/alarms/{id}/acknowledge", 999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void unsupportedEquipmentActionsAreRejectedBeforeSideEffects() throws Exception {
        mockMvc.perform(post("/api/equipment/{id}/command", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"EXPLODE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("action: Unsupported command"));

        mockMvc.perform(post("/api/equipment/999999/command")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"RESET"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void healthEndpointReportsDatabaseReadiness() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }
}
