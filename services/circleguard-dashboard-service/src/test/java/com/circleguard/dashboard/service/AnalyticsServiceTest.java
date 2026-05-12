package com.circleguard.dashboard.service;

import com.circleguard.dashboard.model.AnalyticsData;
import com.circleguard.dashboard.repository.AnalyticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para AnalyticsService.
 * Valida el cálculo de estadísticas y k-anonimato.
 */
public class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAnalyticsDataReturnsNonNullData() {
        UUID circleId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        List<AnalyticsData> mockData = new ArrayList<>();
        mockData.add(AnalyticsData.builder()
                .circleId(circleId)
                .date(LocalDate.now())
                .childCount(5)
                .guardianCount(2)
                .build());

        when(analyticsRepository.findByCircleIdAndDateBetween(circleId, startDate, endDate))
                .thenReturn(mockData);

        List<AnalyticsData> result = analyticsService.getAnalyticsData(circleId, startDate, endDate);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetAnalyticsDataReturnsEmptyListWhenNoData() {
        UUID circleId = UUID.randomUUID();
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();

        when(analyticsRepository.findByCircleIdAndDateBetween(circleId, startDate, endDate))
                .thenReturn(new ArrayList<>());

        List<AnalyticsData> result = analyticsService.getAnalyticsData(circleId, startDate, endDate);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testApplyKAnonymityFilterRemovesDataBelowThreshold() {
        List<AnalyticsData> analyticsData = new ArrayList<>();
        analyticsData.add(AnalyticsData.builder()
                .childCount(2) // Debajo del umbral de k-anonimato
                .guardianCount(1)
                .build());
        analyticsData.add(AnalyticsData.builder()
                .childCount(10) // Encima del umbral
                .guardianCount(5)
                .build());

        KAnonymityFilter filter = new KAnonymityFilter();
        List<AnalyticsData> filtered = filter.applyFilter(analyticsData, 3);

        assertTrue(filtered.stream().allMatch(data -> data.getChildCount() >= 3));
    }

    @Test
    void testCalculateAverageChildrenPerGuardian() {
        UUID circleId = UUID.randomUUID();
        List<AnalyticsData> mockData = new ArrayList<>();
        mockData.add(AnalyticsData.builder()
                .childCount(6)
                .guardianCount(2)
                .build());
        mockData.add(AnalyticsData.builder()
                .childCount(9)
                .guardianCount(3)
                .build());

        when(analyticsRepository.findByCircleId(circleId))
                .thenReturn(mockData);

        double average = analyticsService.calculateAverageChildrenPerGuardian(circleId);

        assertEquals(3.0, average, 0.01);
    }

    @Test
    void testCalculateAverageChildrenPerGuardianWithEmptyData() {
        UUID circleId = UUID.randomUUID();

        when(analyticsRepository.findByCircleId(circleId))
                .thenReturn(new ArrayList<>());

        double average = analyticsService.calculateAverageChildrenPerGuardian(circleId);

        assertEquals(0.0, average);
    }
}
