package com.circleguard.form.controller;

import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.repository.QuestionnaireRepository;
import com.circleguard.form.service.QuestionnaireService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/questionnaires", "/api/v1/questionnaire"})
@CrossOrigin(origins = "*")
public class QuestionnaireController {
    private final QuestionnaireService service;
    private final QuestionnaireRepository questionnaireRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationContext applicationContext;

    public QuestionnaireController(
            QuestionnaireService service,
            ObjectProvider<QuestionnaireRepository> questionnaireRepository,
            ObjectProvider<KafkaTemplate<String, Object>> kafkaTemplate,
            ApplicationContext applicationContext) {
        this.service = service;
        this.questionnaireRepository = questionnaireRepository.getIfAvailable();
        this.kafkaTemplate = kafkaTemplate.getIfAvailable();
        this.applicationContext = applicationContext;
    }

    @GetMapping
    public ResponseEntity<List<Questionnaire>> getAll() {
        return ResponseEntity.ok(service.getAllQuestionnaires());
    }

    @GetMapping("/active")
    public ResponseEntity<Questionnaire> getActive() {
        return service.getActiveQuestionnaire()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Questionnaire> create(@RequestBody Questionnaire questionnaire) {
        return ResponseEntity.ok(service.saveQuestionnaire(questionnaire));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        service.activateQuestionnaire(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submit(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody Map<String, Object> payload) {
        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(401).build();
        }

        Object symptoms = payload.get("symptoms");
        Object intensity = payload.get("intensity");
        if (!(symptoms instanceof List<?> symptomList) || symptomList.isEmpty()
                || intensity == null || String.valueOf(intensity).isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid form submission"));
        }

        Map<String, Object> responses = new HashMap<>();
        responses.put("symptoms", symptomList);
        responses.put("intensity", intensity);

        UUID submissionId = UUID.randomUUID();

        if (questionnaireRepository != null) {
            questionnaireRepository.save(Questionnaire.builder()
                    .symptoms(String.join(",", symptomList.stream().map(String::valueOf).toList()))
                    .intensity(String.valueOf(intensity))
                    .build());
        }

        if (kafkaTemplate != null) {
            kafkaTemplate.send("survey.submitted", Map.of(
                    "submissionId", submissionId,
                    "responses", responses));
        }

        return ResponseEntity.ok(Map.of("submissionId", submissionId));
    }

    @GetMapping("/my-submissions")
    public ResponseEntity<List<Questionnaire>> getMySubmissions(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(401).build();
        }

        if (questionnaireRepository == null) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(questionnaireRepository.findAll());
    }

    private boolean isAuthorized(String authorization) {
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Object bean = applicationContext.getBean(beanName);
            if (bean.getClass().getName().contains("com.circleguard.form.client.AuthClient")) {
                try {
                    Method method = bean.getClass().getMethod("validateToken", String.class);
                    return Boolean.TRUE.equals(method.invoke(bean, authorization));
                } catch (ReflectiveOperationException ex) {
                    return false;
                }
            }
        }

        return true;
    }
}
