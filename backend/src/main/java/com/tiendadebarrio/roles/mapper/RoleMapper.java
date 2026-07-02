package com.tiendadebarrio.roles.mapper;

import com.tiendadebarrio.roles.dto.RoleResponse;
import com.tiendadebarrio.roles.entity.Role;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public RoleResponse toResponse(Role role) {
        if (role == null) {
            return null;
        }
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .active(role.isActive())
                .build();
    }
}
