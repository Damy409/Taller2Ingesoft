package com.circleguard.form.integration;

import com.circleguard.form.client.AuthClient;
import com.circleguard.form.event.FormSubmittedEvent;
import com.circleguard.form.service.QuestionnaireService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Pruebas de integración: Form Service -> Auth Service + Kafka
 * Valida el envío de formularios con eventos y autenticación.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class FormServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QuestionnaireService questionnaireService;

    @MockBean
    private AuthClient authClient;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private UUID userId;
    private String authToken;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        authToken = "Bearer token-" + UUID.randomUUID();
    }

    @Test
    void testSubmitFormWithValidAuthentication() throws Exception {
        Map<String, Object> formData = new HashMap<>();
        formData.put("symptoms", new String[] { "fever", "cough" });
        formData.put("intensity", "moderate");

        when(authClient.validateToken(authToken))
                .thenReturn(true);

        String requestBody = "{\"symptoms\":[\"fever\",\"cough\"],\"intensity\":\"moderate\"}";

        mockMvc.perform(post("/api/v1/questionnaire/submit")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").exists());

        verify(authClient, times(1)).validateToken(authToken);
    }

    @Test
    void testSubmitFormFailsWithInvalidAuthentication() throws Exception {
        when(authClient.validateToken(authToken))
                .thenReturn(false);

        String requestBody = "{\"symptoms\":[\"fever\"],\"intensity\":\"mild\"}";

        mockMvc.perform(post("/api/v1/questionnaire/submit")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testFormSubmissionPublishesToKafka() throws Exception {
        Map<String, Object> formData = new HashMap<>();
        formData.put("symptoms", new String[] { "fever" });
        formData.put("intensity", "severe");

        when(authClient.validateToken(authToken))
                .thenReturn(true);

        String requestBody = "{\"symptoms\":[\"fever\"],\"intensity\":\"severe\"}";

        mockMvc.perform(post("/api/v1/questionnaire/submit")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        verify(kafkaTemplate, times(1)).send(anyString(), any());
    }

    @Test
    void testMultipleFormSubmissions() throws Exception {
        when(authClient.validateToken(authToken))
                .thenReturn(true);

        for (int i = 0; i < 3; i++) {
            String requestBody = "{\"symptoms\":[\"symptom" + i + "\"],\"intensity\":\"mild\"}";

            mockMvc.perform(post("/api/v1/questionnaire/submit")
                    .header("Authorization", authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk());
        }

        verify(kafkaTemplate, times(3)).send(anyString(), any());
    }
}
