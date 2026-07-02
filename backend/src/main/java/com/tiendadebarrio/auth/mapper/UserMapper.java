package com.tiendadebarrio.auth.mapper;

import com.tiendadebarrio.auth.dto.LoginResponse;
import com.tiendadebarrio.auth.dto.UserProfileResponse;
import com.tiendadebarrio.users.entity.AppUser;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public LoginResponse.UserSummary toUserSummary(AppUser user) {
        return LoginResponse.UserSummary.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().getName())
                .build();
    }

    public UserProfileResponse toUserProfile(AppUser user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().getName())
                .active(user.isActive())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
