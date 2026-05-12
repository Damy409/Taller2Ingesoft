package com.circleguard.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsServiceTest {

    @Test
    void shouldThrowExceptionWhenUserNotFound() {

        CustomUserDetailsService service =
                new CustomUserDetailsService(null);

        assertThrows(Exception.class, () ->
                service.loadUserByUsername("user"));
    }
}