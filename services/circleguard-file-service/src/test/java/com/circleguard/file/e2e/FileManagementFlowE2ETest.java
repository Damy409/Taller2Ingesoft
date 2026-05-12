package com.circleguard.file.e2e;

import com.circleguard.file.client.AuthClient;
import com.circleguard.file.model.FileMetadata;
import com.circleguard.file.repository.FileMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas E2E: Flujo completo de gestión de archivos
 * Valida: Carga -> Almacenamiento -> Descarga -> Validación
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") 
public class FileManagementFlowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthClient authClient;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    private UUID userId;
    private String authToken;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        authToken = "Bearer token-" + UUID.randomUUID();
        fileMetadataRepository.deleteAll();
    }

    @Test
    void testCompleteFileUploadFlow() throws Exception {
        when(authClient.validateToken(authToken))
                .thenReturn(true);

        // Step 1: Upload file
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "important_document.pdf",
                "application/pdf",
                "PDF file content".getBytes());

        // Step 2: Store file (mock using multipart form)
        org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder builder = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .multipart("/api/v1/files/upload");

        // Step 3: Verify upload success
        mockMvc.perform(builder
                .file(file)
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileId").exists());

        verify(authClient, times(1)).validateToken(authToken);
    }

    @Test
    void testMultipleFileUploadSequence() throws Exception {
        when(authClient.validateToken(authToken))
                .thenReturn(true);

        String[] fileNames = {
                "document1.pdf",
                "document2.docx",
                "document3.xlsx"
        };

        // Upload multiple files
        for (String fileName : fileNames) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    fileName,
                    "application/octet-stream",
                    ("Content of " + fileName).getBytes());

            org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder builder = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .multipart("/api/v1/files/upload");

            mockMvc.perform(builder
                    .file(file)
                    .header("Authorization", authToken))
                    .andExpect(status().isOk());
        }

        verify(authClient, atLeastOnce()).validateToken(authToken);
    }

    @Test
    void testFileDownloadAfterUpload() throws Exception {
        when(authClient.validateToken(authToken))
                .thenReturn(true);

        String fileName = "downloadable.pdf";
        String fileContent = "Downloadable content";

        // Create mock file record
        FileMetadata metadata = FileMetadata.builder()
                .fileName(fileName)
                .userId(userId)
                .contentType("application/pdf")
                .build();

        fileMetadataRepository.save(metadata);

        String fileId = metadata.getId().toString();

        // Step: Download file
        mockMvc.perform(get("/api/v1/files/download/" + fileId)
                .header("Authorization", authToken))
                .andExpect(status().isOk());

        verify(authClient, times(1)).validateToken(authToken);
    }

    @Test
    void testFileUploadWithInvalidType() throws Exception {
        when(authClient.validateToken(authToken))
                .thenReturn(true);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malware.exe",
                "application/x-msdownload",
                "Executable content".getBytes());

        org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder builder = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .multipart("/api/v1/files/upload");

        mockMvc.perform(builder
                .file(file)
                .header("Authorization", authToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testFileListingForUser() throws Exception {
        when(authClient.validateToken(authToken))
                .thenReturn(true);

        // Create file records for user
        for (int i = 0; i < 3; i++) {
            FileMetadata metadata = FileMetadata.builder()
                    .fileName("file" + i + ".pdf")
                    .userId(userId)
                    .contentType("application/pdf")
                    .build();
            fileMetadataRepository.save(metadata);
        }

        // Retrieve user files
        mockMvc.perform(get("/api/v1/files/my-files")
                .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(authClient, times(1)).validateToken(authToken);
    }
}
