package com.circleguard.identity.service;

import com.circleguard.identity.model.IdentityMapping;
import com.circleguard.identity.repository.IdentityMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para el servicio IdentityVaultService.
 * Valida la generación de IDs anónimos y el mapeo de identidades.
 */
public class IdentityVaultServiceTest {

    @Mock
    private IdentityMappingRepository repository;

    @InjectMocks
    private IdentityVaultService identityVaultService;

    private static final String HASH_SALT = "test-salt";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(identityVaultService, "hashSalt", HASH_SALT);
    }

    @Test
    void testGetOrCreateAnonymousIdReturnsExistingIdWhenMappingExists() {
        String realIdentity = "user@example.com";
        UUID expectedAnonymousId = UUID.randomUUID();
        String hash = computeHash(realIdentity, HASH_SALT);

        IdentityMapping mapping = IdentityMapping.builder()
                .anonymousId(expectedAnonymousId)
                .identityHash(hash)
                .realIdentity(realIdentity)
                .build();

        when(repository.findByIdentityHash(hash)).thenReturn(Optional.of(mapping));

        UUID result = identityVaultService.getOrCreateAnonymousId(realIdentity);

        assertEquals(expectedAnonymousId, result);
        verify(repository, times(1)).findByIdentityHash(hash);
        verify(repository, never()).save(any());
    }

    @Test
    void testGetOrCreateAnonymousIdCreatesNewMappingWhenNotExists() {
        String realIdentity = "newuser@example.com";
        UUID newAnonymousId = UUID.randomUUID();
        String hash = computeHash(realIdentity, HASH_SALT);

        when(repository.findByIdentityHash(hash)).thenReturn(Optional.empty());

        IdentityMapping newMapping = IdentityMapping.builder()
                .anonymousId(newAnonymousId)
                .identityHash(hash)
                .realIdentity(realIdentity)
                .build();

        when(repository.save(any(IdentityMapping.class))).thenReturn(newMapping);

        UUID result = identityVaultService.getOrCreateAnonymousId(realIdentity);

        assertNotNull(result);
        verify(repository, times(1)).findByIdentityHash(hash);
        verify(repository, times(1)).save(any());
    }

    @Test
    void testResolveRealIdentityReturnsValueWhenExists() {
        UUID anonymousId = UUID.randomUUID();
        String realIdentity = "user@example.com";

        IdentityMapping mapping = IdentityMapping.builder()
                .anonymousId(anonymousId)
                .realIdentity(realIdentity)
                .build();

        when(repository.findById(anonymousId)).thenReturn(Optional.of(mapping));

        String result = identityVaultService.resolveRealIdentity(anonymousId);

        assertEquals(realIdentity, result);
    }

    @Test
    void testResolveRealIdentityThrowsExceptionWhenNotFound() {
        UUID anonymousId = UUID.randomUUID();

        when(repository.findById(anonymousId)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> identityVaultService.resolveRealIdentity(anonymousId));
    }

    @Test
    void testGetOrCreateAnonymousIdConsistentHashForSameInput() {
        String realIdentity = "consistent@example.com";
        String hash = computeHash(realIdentity, HASH_SALT);

        UUID firstAnonymousId = UUID.randomUUID();
        IdentityMapping firstMapping = IdentityMapping.builder()
                .anonymousId(firstAnonymousId)
                .identityHash(hash)
                .realIdentity(realIdentity)
                .build();

        when(repository.findByIdentityHash(hash)).thenReturn(Optional.of(firstMapping));

        UUID result1 = identityVaultService.getOrCreateAnonymousId(realIdentity);
        UUID result2 = identityVaultService.getOrCreateAnonymousId(realIdentity);

        assertEquals(result1, result2);
    }

    private String computeHash(String input, String salt) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((input + salt).getBytes());
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hash computation failed", e);
        }
    }
}
