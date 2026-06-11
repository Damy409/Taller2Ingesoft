package com.circleguard.auth.client;

import com.circleguard.auth.resilience.SimpleCircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Component
public class IdentityClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final SimpleCircuitBreaker circuitBreaker;
    private final String identityUrl;
    private final boolean fallbackEnabled;

    public IdentityClient(
            SimpleCircuitBreaker circuitBreaker,
            @Value("${circleguard.identity-service.url:http://localhost:8083/api/v1/identities/map}") String identityUrl,
            @Value("${circleguard.features.identity-fallback:true}") boolean fallbackEnabled) {
        this.circuitBreaker = circuitBreaker;
        this.identityUrl = identityUrl;
        this.fallbackEnabled = fallbackEnabled;
    }

    public UUID getAnonymousId(String realIdentity) {
        return circuitBreaker.execute(
                () -> requestAnonymousId(realIdentity),
                () -> fallbackAnonymousId(realIdentity));
    }

    private UUID requestAnonymousId(String realIdentity) {
        Map<String, String> request = Map.of("realIdentity", realIdentity);
        Map<?, ?> response = restTemplate.postForObject(identityUrl, request, Map.class);
        if (response == null || response.get("anonymousId") == null) {
            throw new IllegalStateException("Identity service did not return anonymousId");
        }
        return UUID.fromString(response.get("anonymousId").toString());
    }

    private UUID fallbackAnonymousId(String realIdentity) {
        if (!fallbackEnabled) {
            throw new IllegalStateException("Identity service unavailable and fallback disabled");
        }
        return UUID.nameUUIDFromBytes(("fallback:" + realIdentity).getBytes(StandardCharsets.UTF_8));
    }
}

