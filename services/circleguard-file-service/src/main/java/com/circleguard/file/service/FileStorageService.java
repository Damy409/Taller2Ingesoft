package com.circleguard.file.service;

import com.circleguard.file.model.FileMetadata;
import com.circleguard.file.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class FileStorageService {
    @Autowired(required = false)
    private FileMetadataRepository fileMetadataRepository;

    public boolean validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) return false;

        String type = file.getContentType();
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();

        if (type == null) {
            return false;
        }

        boolean supportedMimeType = type.equals("application/pdf")
                || type.equals("application/msword")
                || type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        boolean safeOctetStream = type.equals("application/octet-stream")
                && (filename.endsWith(".pdf") || filename.endsWith(".doc") || filename.endsWith(".docx")
                        || filename.endsWith(".xlsx"));

        if (!supportedMimeType && !safeOctetStream) return false;

        long maxSize = 10 * 1024 * 1024; // 10MB
        return file.getSize() <= maxSize;
    }

    public String storeFile(UUID userId, MultipartFile file) {

        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        if (!validateFile(file)) {
            throw new IllegalArgumentException("Invalid file");
        }

        if (fileMetadataRepository == null) {
            return UUID.randomUUID().toString();
        }

        FileMetadata metadata = FileMetadata.builder()
                .fileName(file.getOriginalFilename())
                .userId(userId)
                .contentType(file.getContentType())
                .build();

        return fileMetadataRepository.save(metadata).getId().toString();
    }

    public String saveFile(MultipartFile file) {
        // Simular userId (porque controller no lo manda)
        UUID userId = UUID.randomUUID();
        return storeFile(userId, file);
    }
}
