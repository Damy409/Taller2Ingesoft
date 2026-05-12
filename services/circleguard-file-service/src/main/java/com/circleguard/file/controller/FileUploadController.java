package com.circleguard.file.controller;

import com.circleguard.file.client.AuthClient;
import com.circleguard.file.model.FileMetadata;
import com.circleguard.file.repository.FileMetadataRepository;
import com.circleguard.file.service.FileStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@CrossOrigin(origins = "*")
public class FileUploadController {
    private final FileStorageService storageService;
    private final AuthClient authClient;
    private final FileMetadataRepository fileMetadataRepository;

    public FileUploadController(
            FileStorageService storageService,
            ObjectProvider<AuthClient> authClient,
            ObjectProvider<FileMetadataRepository> fileMetadataRepository) {
        this.storageService = storageService;
        this.authClient = authClient.getIfAvailable();
        this.fileMetadataRepository = fileMetadataRepository.getIfAvailable();
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam("file") MultipartFile file) {
        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(401).build();
        }

        try {
            String fileId = storageService.saveFile(file);
            return ResponseEntity.ok(Map.of("fileId", fileId, "filename", file.getOriginalFilename()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/my-files")
    public ResponseEntity<List<FileMetadata>> listMyFiles(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(401).build();
        }

        if (fileMetadataRepository == null) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(fileMetadataRepository.findAll());
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<byte[]> download(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID fileId) {
        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(401).build();
        }

        if (fileMetadataRepository == null) {
            return ResponseEntity.notFound().build();
        }

        return fileMetadataRepository.findById(fileId)
                .map(metadata -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
                        .body(("Stored file placeholder: " + metadata.getFileName()).getBytes()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean isAuthorized(String authorization) {
        return authClient == null || authClient.validateToken(authorization);
    }
}
