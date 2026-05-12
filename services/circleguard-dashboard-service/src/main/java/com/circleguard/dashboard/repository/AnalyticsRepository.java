package com.circleguard.dashboard.repository;

import com.circleguard.dashboard.model.AnalyticsData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AnalyticsRepository extends JpaRepository<AnalyticsData, UUID> {

    List<AnalyticsData> findByCircleId(UUID circleId);

    List<AnalyticsData> findByCircleIdAndDateBetween(
            UUID circleId,
            LocalDate startDate,
            LocalDate endDate
    );
}