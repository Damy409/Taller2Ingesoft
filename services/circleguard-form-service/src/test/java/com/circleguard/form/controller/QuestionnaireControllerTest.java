package com.circleguard.form.controller;

import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.service.QuestionnaireService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuestionnaireController.class)
class QuestionnaireControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionnaireService questionnaireService;

    @Test
    void shouldReturnActiveQuestionnaire() throws Exception {

        UUID id = UUID.randomUUID();

        Questionnaire q = Questionnaire.builder()
                .id(id)
                .symptoms("fever") // ✅ campo real
                .intensity("mild") // ✅ campo real
                .build();

        Mockito.when(questionnaireService.getActiveQuestionnaire())
                .thenReturn(Optional.of(q));

        mockMvc.perform(get("/api/v1/questionnaires/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symptoms").value("fever")); // ✅ ajustado
    }

    @Test
    void shouldCreateQuestionnaire() throws Exception {

        UUID id = UUID.randomUUID();

        Questionnaire q = Questionnaire.builder()
                .id(id)
                .symptoms("cough") // ✅ campo real
                .intensity("moderate") // ✅ campo real
                .build();

        Mockito.when(questionnaireService.saveQuestionnaire(Mockito.any(Questionnaire.class)))
                .thenReturn(q);

        mockMvc.perform(post("/api/v1/questionnaires")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"symptoms\": \"cough\", \"intensity\": \"moderate\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symptoms").value("cough")); // ✅ ajustado
    }
}