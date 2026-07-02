package com.tiendadebarrio.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class LoginResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final UserSummary user;

    @Getter
    @Builder
    public static class UserSummary {
        private final UUID id;
        private final String username;
        private final String email;
        private final String firstName;
        private final String lastName;
        private final String role;
    }
}
