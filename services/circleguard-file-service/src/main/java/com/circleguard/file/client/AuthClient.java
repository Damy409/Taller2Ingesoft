package com.circleguard.file.client;


import org.springframework.stereotype.Component;

@Component
public class AuthClient {

    /**
     * Simula validación de token
     */
    public boolean validateToken(String token) {
        return token != null && token.startsWith("Bearer ");
    }
}