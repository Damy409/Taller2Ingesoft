package com.circleguard.dashboard.integration;

import com.circleguard.dashboard.client.PromotionClient;
import com.circleguard.dashboard.model.AnalyticsData;
import com.circleguard.dashboard.repository.AnalyticsRepository;
import com.circleguard.dashboard.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Pruebas de integración: Dashboard Service -> Database + Promotion Service
 * Valida la lectura de datos analíticos y comunicación con otros servicios.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @MockBean
    private PromotionClient promotionClient;

    private UUID circleId;

    @BeforeEach
    void setUp() {
        circleId = UUID.randomUUID();
        analyticsRepository.deleteAll();
    }

    @Test
    void testRetrieveAnalyticsDataFromDatabase() {
        AnalyticsData data = AnalyticsData.builder()
                .circleId(circleId)
                .date(LocalDate.now())
                .childCount(5)
                .guardianCount(2)
                .build();

        analyticsRepository.save(data);

        List<AnalyticsData> result = analyticsService.getAnalyticsData(
                circleId,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1));

        assertFalse(result.isEmpty());
        assertEquals(5, result.get(0).getChildCount());
    }

    @Test
    void testAnalyticsApiEndpoint() throws Exception {
        AnalyticsData data = AnalyticsData.builder()
                .circleId(circleId)
                .date(LocalDate.now())
                .childCount(8)
                .guardianCount(3)
                .build();

        analyticsRepository.save(data);

        mockMvc.perform(get("/api/v1/analytics/" + circleId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].childCount").value(8));
    }

    @Test
    void testAnalyticsWithMultipleDataPoints() {
        for (int i = 0; i < 5; i++) {
            AnalyticsData data = AnalyticsData.builder()
                    .circleId(circleId)
                    .date(LocalDate.now().minusDays(i))
                    .childCount(5 + i)
                    .guardianCount(2)
                    .build();
            analyticsRepository.save(data);
        }

        List<AnalyticsData> result = analyticsService.getAnalyticsData(
                circleId,
                LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(1));

        assertEquals(5, result.size());
    }

    @Test
    void testCalculateAverageFromMultipleEntries() {
        analyticsRepository.save(AnalyticsData.builder()
                .circleId(circleId)
                .childCount(6)
                .guardianCount(2)
                .build());
        analyticsRepository.save(AnalyticsData.builder()
                .circleId(circleId)
                .childCount(9)
                .guardianCount(3)
                .build());

        double average = analyticsService.calculateAverageChildrenPerGuardian(circleId);

        assertEquals(3.0, average, 0.01);
    }
}
