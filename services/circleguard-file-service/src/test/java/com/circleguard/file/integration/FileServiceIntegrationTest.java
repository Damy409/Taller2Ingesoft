package com.circleguard.file.integration;

import com.circleguard.file.client.AuthClient;
import com.circleguard.file.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración: File Service -> Auth Service + Storage
 */
@SpringBootTest
@AutoConfigureMockMvc
public class FileServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileStorageService fileStorageService;

    @MockBean
    private AuthClient authClient;

    private UUID userId;
    private String authToken;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        authToken = "Bearer token-" + UUID.randomUUID();
    }

    @Test
    void testUploadFileWithValidAuthentication() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        when(authClient.validateToken(authToken)).thenReturn(true);

        mockMvc.perform(
                multipart("/api/v1/files/upload")
                        .file(file) // ✅ CORRECTO
                        .header("Authorization", authToken)
        )
        .andExpect(status().isOk());

        verify(authClient, times(1)).validateToken(authToken);
    }

    @Test
    void testUploadFileFailsWithInvalidAuthentication() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "PDF content".getBytes()
        );

        when(authClient.validateToken(authToken)).thenReturn(false);

        mockMvc.perform(
                multipart("/api/v1/files/upload")
                        .file(file)
                        .header("Authorization", authToken)
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void testUploadMultipleFilesSuccessfully() throws Exception {

        when(authClient.validateToken(authToken)).thenReturn(true);

        for (int i = 0; i < 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "document" + i + ".pdf",
                    "application/pdf",
                    ("PDF content " + i).getBytes()
            );

            mockMvc.perform(
                    multipart("/api/v1/files/upload")
                            .file(file)
                            .header("Authorization", authToken)
            )
            .andExpect(status().isOk());
        }

        verify(authClient, atLeast(3)).validateToken(authToken);
    }

    @Test
    void testUploadFileStorageRejection() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large_file.iso",
                "application/x-iso9660-image",
                new byte[1024 * 1024 * 100] // 100MB
        );

        when(authClient.validateToken(authToken)).thenReturn(true);

        mockMvc.perform(
                multipart("/api/v1/files/upload")
                        .file(file)
                        .header("Authorization", authToken)
        )
        .andExpect(status().isBadRequest());
    }
}