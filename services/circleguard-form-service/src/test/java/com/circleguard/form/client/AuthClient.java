package com.circleguard.form.client;

import org.springframework.stereotype.Component;

@Component
public class AuthClient {

    public boolean validateToken(String token) {
        return token != null && token.startsWith("Bearer ");
    }
}