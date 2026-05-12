package com.circleguard.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;
    private static final String SECRET = "my-super-secret-dev-key-32-chars-long-12345678";
    private static final long EXPIRATION = 3600000;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(SECRET, EXPIRATION);
    }

    @Test
    void testGenerateTokenWithValidAuthentication() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = mock(Authentication.class);

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        when(auth.getAuthorities()).thenReturn((Collection) authorities);

        String token = jwtTokenService.generateToken(anonymousId, auth);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }

    @Test
    void testGenerateTokenSubjectIsAnonymousId() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = mock(Authentication.class);

        when(auth.getAuthorities()).thenReturn((Collection) new ArrayList<>());

        String token = jwtTokenService.generateToken(anonymousId, auth);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertEquals(anonymousId.toString(), claims.getSubject());
    }

    @Test
    void testGenerateTokenIncludesPermissions() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = mock(Authentication.class);

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        authorities.add(new SimpleGrantedAuthority("ROLE_GUARDIAN"));

        when(auth.getAuthorities()).thenReturn((Collection) authorities);

        String token = jwtTokenService.generateToken(anonymousId, auth);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        List<?> permissions = claims.get("permissions", List.class);

        assertNotNull(permissions);
        assertEquals(2, permissions.size());
    }

    @Test
    void testGenerateTokenIsNotExpiredImmediately() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = mock(Authentication.class);

        when(auth.getAuthorities()).thenReturn((Collection) new ArrayList<>());

        String token = jwtTokenService.generateToken(anonymousId, auth);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        Date expiration = claims.getExpiration();
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void testGenerateTokenWithEmptyAuthorities() {
        UUID anonymousId = UUID.randomUUID();
        Authentication auth = mock(Authentication.class);

        when(auth.getAuthorities()).thenReturn((Collection) new ArrayList<>());

        String token = jwtTokenService.generateToken(anonymousId, auth);

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();

        List<?> permissions = claims.get("permissions", List.class);

        assertTrue(permissions.isEmpty());
    }
}