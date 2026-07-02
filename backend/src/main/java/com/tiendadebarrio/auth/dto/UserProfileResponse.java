package com.tiendadebarrio.auth.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserProfileResponse {

    private final UUID id;
    private final String username;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String role;
    private final boolean active;
    private final LocalDateTime lastLoginAt;
}
