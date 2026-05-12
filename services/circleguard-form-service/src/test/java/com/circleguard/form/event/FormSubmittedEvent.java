package com.circleguard.form.event;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormSubmittedEvent {

    private UUID submissionId;
    private UUID userId;
    private String intensity;
}