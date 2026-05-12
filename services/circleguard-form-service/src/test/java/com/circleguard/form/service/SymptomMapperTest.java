package com.circleguard.form.service;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.Question;
import com.circleguard.form.model.QuestionType;
import com.circleguard.form.model.Questionnaire;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SymptomMapperTest {

    private final SymptomMapper mapper = new SymptomMapper();

    @Test
    void shouldDetectSymptomsFromFever() {

        HealthSurvey survey = HealthSurvey.builder()
                .responses(Map.of("fever", "YES"))
                .build();

        Questionnaire questionnaire = Questionnaire.builder()
                .symptoms("fever")
                .build();

        assertTrue(mapper.hasSymptoms(survey, questionnaire));
    }

    @Test
    void shouldNotDetectSymptomsWhenNo() {

        HealthSurvey survey = HealthSurvey.builder()
                .responses(Map.of("fever", "NO"))
                .build();

        Questionnaire questionnaire = Questionnaire.builder()
                .symptoms("fever")
                .build();

        assertFalse(mapper.hasSymptoms(survey, questionnaire));
    }
}
