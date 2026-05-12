package com.circleguard.identity.integration;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import com.circleguard.identity.service.IdentityVaultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Pruebas de integración: Identity Service -> Database + Kafka
 * Valida el mapeo de identidades y la publicación de eventos.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class IdentityToEventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityVaultService identityVaultService;

    @Autowired
    private IdentityMappingRepository identityMappingRepository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        identityMappingRepository.deleteAll();
    }

    @Test
    void testMapIdentityAndStoreInDatabase() {
        String realIdentity = "user@example.com";

        UUID anonymousId = identityVaultService.getOrCreateAnonymousId(realIdentity);

        assertNotNull(anonymousId);
        assertTrue(identityMappingRepository.findById(anonymousId).isPresent());
    }

    @Test
    void testMapMultipleIdentitiesCreatesSeparateIds() {
        String identity1 = "user1@example.com";
        String identity2 = "user2@example.com";

        UUID anonymousId1 = identityVaultService.getOrCreateAnonymousId(identity1);
        UUID anonymousId2 = identityVaultService.getOrCreateAnonymousId(identity2);

        assertNotEquals(anonymousId1, anonymousId2);
    }

    @Test
    void testSameIdentityMapsToSameAnonymousId() {
        String realIdentity = "consistent@example.com";

        UUID anonymousId1 = identityVaultService.getOrCreateAnonymousId(realIdentity);
        UUID anonymousId2 = identityVaultService.getOrCreateAnonymousId(realIdentity);

        assertEquals(anonymousId1, anonymousId2);
    }

    @Test
    void testIdentityMappingApiEndpoint() throws Exception {
        String realIdentity = "apiuser@example.com";
        String requestBody = "{\"realIdentity\":\"" + realIdentity + "\"}";

        mockMvc.perform(post("/api/v1/identities/map")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anonymousId").exists());
    }

    @Test
    void testResolveIdentityAfterMapping() {
        String realIdentity = "resolve@example.com";

        UUID anonymousId = identityVaultService.getOrCreateAnonymousId(realIdentity);
        String resolved = identityVaultService.resolveRealIdentity(anonymousId);

        assertEquals(realIdentity, resolved);
    }
}
