package com.circleguard.auth.e2e;

import com.circleguard.auth.client.IdentityClient;
import com.circleguard.auth.service.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
    properties = {
        "jwt.secret=my-super-secret-test-key-32-chars-long",
        "jwt.expiration=3600000",

        
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration"
    }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationFlowE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IdentityClient identityClient;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private org.springframework.ldap.core.LdapTemplate ldapTemplate;

    private UUID anonymousId;

    @BeforeEach
    void setUp() {
        anonymousId = UUID.randomUUID();
    }

    @Test
    void testCompleteAuthenticationFlow() throws Exception {

        when(identityClient.getAnonymousId(any()))
                .thenReturn(anonymousId);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user",
                "pass"
        );

        when(authenticationManager.authenticate(any()))
                .thenReturn(auth);

        when(jwtTokenService.generateToken(any(), any()))
                .thenReturn("fake-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-token"))
                .andExpect(jsonPath("$.anonymousId").value(anonymousId.toString()));
    }
}