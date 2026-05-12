package com.circleguard.form.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Questionnaire {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId;

    private String symptoms;

    private String intensity;

    private LocalDate submissionDate;
}