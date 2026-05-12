package com.circleguard.file.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {

    @Id
    @GeneratedValue
    private UUID id;

    private String fileName;

    private UUID userId;

    private String contentType;
}