package com.circleguard.form.e2e;

import com.circleguard.form.client.AuthClient;
import com.circleguard.form.event.FormSubmittedEvent;
import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.repository.QuestionnaireRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas E2E: Flujo completo de formularios
 * Valida: Presentación -> Almacenamiento -> Eventos
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // 👈 obligatorio
public class FormSubmissionFlowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthClient authClient;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private QuestionnaireRepository questionnaireRepository;

    private UUID userId;
    private String authToken;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        authToken = "Bearer token-" + UUID.randomUUID();
        questionnaireRepository.deleteAll();
    }

    @Test
    void testCompleteFormSubmissionFlow() throws Exception {
        when(authClient.validateToken(authToken))
                .thenReturn(true);

        // Step 1: Create form submission
        String formData = "{\"symptoms\":[\"fever\",\"cough\"],\"intensity\":\"severe\",\"date\":\"2026-05-10\"}";

        // Step 2: Submit form
        MvcResult result = mockMvc.perform(post("/api/v1/questionnaire/submit")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(formData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").exists())
                .andReturn();

        // Step 3: Verify event published
        verify(kafkaTemplate, times(1)).send(anyString(), any());

        // Step 4: Verify stored in database
        long count = questionnaireRepository.count();
        assertTrue(count > 0);
    }

    @Test
    void testMultipleFormSubmissions() throws Exception {
        when(authClient.validateToken(authToken))
                .thenReturn(true);

        String[] symptoms = {
                "{\"symptoms\":[\"fever\"],\"intensity\":\"mild\"}",
                "{\"symptoms\":[\"cough\",\"fatigue\"],\"intensity\":\"moderate\"}",
                "{\"symptoms\":[\"fever\",\"cough\",\"headache\"],\"intensity\":\"severe\"}"
        };

        // Submit multiple forms
        for (String symptomData : symptoms) {
            mockMvc.perform(post("/api/v1/questionnaire/submit")
                    .header("Authorization", authToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(symptomData))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.submissionId").exists());
        }

        // Verify all stored
        long count = questionnaireRepository.count();
        assertEquals(3, count);

        // Verify events published
        verify(kafkaTemplate, times(3)).send(anyString(), any());
    }

    @Test
    void testFormValidation() throws Exception {
        when(authClient.validateToken(authToken))
                .thenReturn(true);

        // Invalid form data
        String invalidData = "{\"symptoms\":[],\"intensity\":\"\"}";

        mockMvc.perform(post("/api/v1/questionnaire/submit")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidData))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testFormRetrievalByUser() throws Exception {
        when(authClient.validateToken(authToken))
                .thenReturn(true);

        // Submit form first
        String formData = "{\"symptoms\":[\"fever\"],\"intensity\":\"moderate\"}";

        mockMvc.perform(post("/api/v1/questionnaire/submit")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(formData))
                .andExpect(status().isOk());

        // Retrieve user's forms
        mockMvc.perform(get("/api/v1/questionnaire/my-submissions")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(authClient, atLeast(2)).validateToken(authToken);
    }

    @Test
    void testFormSubmissionWithDateTime() throws Exception {
        when(authClient.validateToken(authToken))
                .thenReturn(true);

        LocalDate submissionDate = LocalDate.now();
        String formData = "{\"symptoms\":[\"fever\"],\"intensity\":\"mild\",\"submissionDate\":\"" + submissionDate
                + "\"}";

        MvcResult result = mockMvc.perform(post("/api/v1/questionnaire/submit")
                .header("Authorization", authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(formData))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("submissionId"));
    }
}
