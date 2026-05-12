package com.circleguard.dashboard.service;

import com.circleguard.dashboard.model.AnalyticsData;
import com.circleguard.dashboard.repository.AnalyticsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    /**
     * Obtiene datos analíticos por rango de fechas
     */
    public List<AnalyticsData> getAnalyticsData(UUID circleId, LocalDate startDate, LocalDate endDate) {

        // Si no hay fechas → traer todo
        if (startDate == null || endDate == null) {
            return analyticsRepository.findByCircleId(circleId);
        }

        return analyticsRepository.findByCircleIdAndDateBetween(circleId, startDate, endDate);
    }

        /**
     * Estadísticas globales del sistema
     */
    public Map<String, Object> getGlobalHealthStats() {
        Map<String, Object> stats = new HashMap<>();

        // Valores dummy (puedes luego conectar a DB real)
        stats.put("totalGreen", 1500);
        stats.put("totalExposed", 45);

        return stats;
    }

    /**
     * Tendencias por ubicación (simulado)
     */
    public List<Map<String, Object>> getEntryTrends(UUID locationId) {
        List<Map<String, Object>> trends = new ArrayList<>();

        Map<String, Object> entry = new HashMap<>();
        entry.put("hour", "08:00");
        entry.put("count", 120);

        trends.add(entry);

        return trends;
    }

        /**
         * Calcula promedio de niños por acudiente
         */
        public double calculateAverageChildrenPerGuardian(UUID circleId) {

            List<AnalyticsData> dataList = analyticsRepository.findByCircleId(circleId);

            if (dataList.isEmpty()) {
                return 0.0;
            }

            double totalChildren = 0;
            double totalGuardians = 0;

            for (AnalyticsData data : dataList) {
                totalChildren += data.getChildCount();
                totalGuardians += data.getGuardianCount();
            }

            if (totalGuardians == 0) {
                return 0.0;
            }

            return totalChildren / totalGuardians;
        }
    
    /**
     * Resumen general del campus
     */
    public Map<String, Object> getCampusSummary() {
        Map<String, Object> summary = new HashMap<>();

        summary.put("totalStudents", 5000);
        summary.put("activeUsers", 4200);
        summary.put("alerts", 12);

        return summary;
    }

    /**
     * Estadísticas por departamento
     */
    public Map<String, Object> getDepartmentStats(String department) {
        Map<String, Object> stats = new HashMap<>();

        stats.put("department", department);
        stats.put("totalUsers", 300);
        stats.put("incidents", 5);

        return stats;
    }

    /**
     * Serie de tiempo (simulada)
     */
    public List<Map<String, Object>> getTimeSeries(String period, int limit) {
        List<Map<String, Object>> series = new ArrayList<>();

        for (int i = 0; i < limit; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("label", period + "-" + i);
            point.put("value", 100 + i);

            series.add(point);
        }

        return series;
    }
}