package com.circleguard.identity.e2e;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import com.circleguard.identity.service.IdentityVaultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas E2E: Flujo completo de gestión de identidades
 * Valida: Mapeo de identidad -> Almacenamiento -> Resolución
 */
@SpringBootTest
@AutoConfigureMockMvc
public class IdentityManagementFlowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityVaultService identityVaultService;

    @Autowired
    private IdentityMappingRepository identityMappingRepository;

    @BeforeEach
    void setUp() {
        identityMappingRepository.deleteAll();
    }

    @Test
    void testCompleteIdentityMappingFlow() throws Exception {
        String realIdentity = "user@circleguard.edu";

        // Step 1: Map identity via API
        String mapRequest = "{\"realIdentity\":\"" + realIdentity + "\"}";

        MvcResult mapResult = mockMvc.perform(post("/api/v1/identities/map")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anonymousId").exists())
                .andReturn();

        // Step 2: Extract anonymous ID
        String anonymousId = extractAnonymousIdFromResponse(mapResult);

        // Step 3: Verify mapping is stored
        assertTrue(identityMappingRepository.findById(UUID.fromString(anonymousId)).isPresent());

        // Step 4: Resolve identity
        String resolved = identityVaultService.resolveRealIdentity(UUID.fromString(anonymousId));
        assertEquals(realIdentity, resolved);
    }

    @Test
    void testIdentityConsistency() throws Exception {
        String realIdentity = "guardian@circleguard.edu";

        // First mapping
        String mapRequest = "{\"realIdentity\":\"" + realIdentity + "\"}";

        MvcResult result1 = mockMvc.perform(post("/api/v1/identities/map")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapRequest))
                .andExpect(status().isOk())
                .andReturn();

        String anonymousId1 = extractAnonymousIdFromResponse(result1);

        // Second mapping (same identity)
        MvcResult result2 = mockMvc.perform(post("/api/v1/identities/map")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapRequest))
                .andExpect(status().isOk())
                .andReturn();

        String anonymousId2 = extractAnonymousIdFromResponse(result2);

        // Verify same mapping
        assertEquals(anonymousId1, anonymousId2);
    }

    @Test
    void testMultipleIdentityMappings() throws Exception {
        String[] identities = {
                "user1@circleguard.edu",
                "user2@circleguard.edu",
                "user3@circleguard.edu"
        };

        String[] anonymousIds = new String[3];

        // Map multiple identities
        for (int i = 0; i < identities.length; i++) {
            String mapRequest = "{\"realIdentity\":\"" + identities[i] + "\"}";

            MvcResult result = mockMvc.perform(post("/api/v1/identities/map")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapRequest))
                    .andExpect(status().isOk())
                    .andReturn();

            anonymousIds[i] = extractAnonymousIdFromResponse(result);
        }

        // Verify all are different
        for (int i = 0; i < anonymousIds.length; i++) {
            for (int j = i + 1; j < anonymousIds.length; j++) {
                assertNotEquals(anonymousIds[i], anonymousIds[j]);
            }
        }

        // Verify all stored
        for (String anonymousId : anonymousIds) {
            assertTrue(identityMappingRepository.findById(UUID.fromString(anonymousId)).isPresent());
        }
    }

    @Test
    void testVisitorRegistrationFlow() throws Exception {
        String visitorName = "Temporary Visitor";
        String visitorEmail = "visitor@temporary.edu";

        String visitorRequest = "{\"name\":\"" + visitorName + "\",\"email\":\"" + visitorEmail + "\"}";

        MvcResult result = mockMvc.perform(post("/api/v1/identities/visitor")
                .contentType(MediaType.APPLICATION_JSON)
                .content(visitorRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anonymousId").exists())
                .andReturn();

        String anonymousId = extractAnonymousIdFromResponse(result);

        // Verify mapping exists
        assertTrue(identityMappingRepository.findById(UUID.fromString(anonymousId)).isPresent());
    }

    @Test
    void testIdentityResolveEndpoint() throws Exception {
        String realIdentity = "resolver@circleguard.edu";

        // Map identity
        UUID mappedId = identityVaultService.getOrCreateAnonymousId(realIdentity);

        // Attempt to resolve (might be restricted in production)
        mockMvc.perform(get("/api/v1/identities/resolve/" + mappedId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realIdentity").exists());
    }

    private String extractAnonymousIdFromResponse(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        int startIndex = content.indexOf("\"anonymousId\":\"") + 15;
        int endIndex = content.indexOf("\"", startIndex);
        return content.substring(startIndex, endIndex);
    }
}
