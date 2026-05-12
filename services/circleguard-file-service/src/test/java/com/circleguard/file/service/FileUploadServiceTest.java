package com.circleguard.file.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para FileStorageService.
 */
public class FileUploadServiceTest {

    @InjectMocks
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testValidateFileWithValidType() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "PDF content".getBytes());

        assertTrue(fileStorageService.validateFile(file));
    }

    @Test
    void testValidateFileWithInvalidType() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/x-msdownload",
                "executable content".getBytes());

        assertFalse(fileStorageService.validateFile(file));
    }

    @Test
    void testValidateFileWithValidSize() {
        byte[] content = new byte[1024 * 100]; // 100 KB

        MultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                content);

        assertTrue(fileStorageService.validateFile(file));
    }

    @Test
    void testValidateFileWithExceededSize() {
        byte[] content = new byte[1024 * 1024 * 50]; // 50 MB

        MultipartFile file = new MockMultipartFile(
                "file",
                "large_file.pdf",
                "application/pdf",
                content);

        assertFalse(fileStorageService.validateFile(file));
    }

    @Test
    void testStoreFileReturnsId() {
        UUID userId = UUID.randomUUID();

        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes());

        String result = fileStorageService.storeFile(userId, file);

        assertNotNull(result);
    }

    @Test
    void testStoreFileWithNullFile() {
        UUID userId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () ->
                fileStorageService.storeFile(userId, null));
    }
}