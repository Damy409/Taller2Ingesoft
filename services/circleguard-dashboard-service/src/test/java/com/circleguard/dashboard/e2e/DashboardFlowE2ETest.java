package com.circleguard.dashboard.e2e;

import com.circleguard.dashboard.model.AnalyticsData;
import com.circleguard.dashboard.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas E2E: Flujo completo de dashboard
 * Valida: Creación de datos analíticos -> Visualización -> Filtrado
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") 
public class DashboardFlowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    private UUID circleId;

    @BeforeEach
    void setUp() {
        circleId = UUID.randomUUID();
        analyticsRepository.deleteAll();
    }

    @Test
    void testCompleteDashboardFlow() throws Exception {
        // Step 1: Create initial analytics data
        AnalyticsData data1 = AnalyticsData.builder()
                .circleId(circleId)
                .date(LocalDate.now())
                .childCount(8)
                .guardianCount(3)
                .build();

        AnalyticsData data2 = AnalyticsData.builder()
                .circleId(circleId)
                .date(LocalDate.now().minusDays(1))
                .childCount(7)
                .guardianCount(3)
                .build();

        analyticsRepository.save(data1);
        analyticsRepository.save(data2);

        // Step 2: Retrieve analytics
        MvcResult result = mockMvc.perform(get("/api/v1/analytics/" + circleId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].childCount").exists())
                .andReturn();

        // Step 3: Verify data
        assertNotNull(result.getResponse().getContentAsString());
        assertTrue(result.getResponse().getContentAsString().contains("childCount"));
    }

    @Test
    void testDashboardDataAggregation() throws Exception {
        // Create multiple data points
        for (int i = 0; i < 10; i++) {
            AnalyticsData data = AnalyticsData.builder()
                    .circleId(circleId)
                    .date(LocalDate.now().minusDays(i))
                    .childCount(5 + i)
                    .guardianCount(2)
                    .build();
            analyticsRepository.save(data);
        }

        // Retrieve aggregated data
        MvcResult result = mockMvc.perform(get("/api/v1/analytics/" + circleId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("childCount"));
    }

    @Test
    void testDashboardDateRangeFilter() throws Exception {
        // Create data across date range
        for (int i = 0; i < 30; i++) {
            AnalyticsData data = AnalyticsData.builder()
                    .circleId(circleId)
                    .date(LocalDate.now().minusDays(i))
                    .childCount(10)
                    .guardianCount(3)
                    .build();
            analyticsRepository.save(data);
        }

        // Query with date range
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        MvcResult result = mockMvc.perform(
                get("/api/v1/analytics/" + circleId + "/range")
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertNotNull(content);
    }

    @Test
    void testDashboardComparisonMetrics() throws Exception {
        // Create data with variation
        analyticsRepository.save(AnalyticsData.builder()
                .circleId(circleId)
                .date(LocalDate.now().minusDays(1))
                .childCount(5)
                .guardianCount(2)
                .build());

        analyticsRepository.save(AnalyticsData.builder()
                .circleId(circleId)
                .date(LocalDate.now())
                .childCount(10)
                .guardianCount(3)
                .build());

        // Get metrics
        MvcResult result = mockMvc.perform(
                get("/api/v1/analytics/" + circleId + "/metrics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("average") || content.contains("metrics"));
    }
}
