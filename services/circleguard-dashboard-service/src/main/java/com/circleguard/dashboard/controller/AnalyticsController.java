package com.circleguard.dashboard.controller;

import com.circleguard.dashboard.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1/analytics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    @GetMapping("/trends/{locationId}")
    public ResponseEntity<List<Map<String, Object>>> getTrends(@PathVariable UUID locationId) {
        return ResponseEntity.ok(analyticsService.getEntryTrends(locationId));
    }

    @GetMapping("/{circleId}")
    public ResponseEntity<?> getAnalytics(@PathVariable UUID circleId) {
        return ResponseEntity.ok(analyticsService.getAnalyticsData(circleId, null, null));
    }

    @GetMapping("/{circleId}/range")
    public ResponseEntity<?> getAnalyticsByDateRange(
            @PathVariable UUID circleId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return ResponseEntity.ok(analyticsService.getAnalyticsData(circleId, startDate, endDate));
    }

    @GetMapping("/{circleId}/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics(@PathVariable UUID circleId) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("averageChildrenPerGuardian", analyticsService.calculateAverageChildrenPerGuardian(circleId));
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/health-board")
    public ResponseEntity<Map<String, Object>> getHealthBoardStats() {
        return ResponseEntity.ok(analyticsService.getGlobalHealthStats());
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(analyticsService.getCampusSummary());
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<Map<String, Object>> getDepartmentStats(@PathVariable String department) {
        return ResponseEntity.ok(analyticsService.getDepartmentStats(department));
    }

    @GetMapping("/time-series")
    public ResponseEntity<List<Map<String, Object>>> getTimeSeries(
            @RequestParam(defaultValue = "hourly") String period,
            @RequestParam(defaultValue = "24") int limit) {
        return ResponseEntity.ok(analyticsService.getTimeSeries(period, limit));
    }
}
