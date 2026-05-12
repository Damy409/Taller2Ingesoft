package com.circleguard.dashboard.model;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsData {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID circleId;

    private LocalDate date;

    private int childCount;
    private int guardianCount;
}