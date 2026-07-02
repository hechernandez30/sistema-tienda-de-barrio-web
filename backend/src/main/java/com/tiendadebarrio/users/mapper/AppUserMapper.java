package com.tiendadebarrio.users.mapper;

import com.tiendadebarrio.roles.mapper.RoleMapper;
import com.tiendadebarrio.users.dto.UserListResponse;
import com.tiendadebarrio.users.dto.UserResponse;
import com.tiendadebarrio.users.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppUserMapper {

    private final RoleMapper roleMapper;

    public UserResponse toResponse(AppUser user) {
        return UserResponse.builder()
                .id(user.getId())
                .role(roleMapper.toResponse(user.getRole()))
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .active(user.isActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public UserListResponse toListResponse(AppUser user) {
        return UserListResponse.builder()
                .id(user.getId())
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .active(user.isActive())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
