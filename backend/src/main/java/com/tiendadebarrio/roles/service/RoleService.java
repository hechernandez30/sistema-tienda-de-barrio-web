package com.tiendadebarrio.roles.service;

import com.tiendadebarrio.audit.service.AuditService;
import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.roles.dto.RoleCreateRequest;
import com.tiendadebarrio.roles.dto.RoleResponse;
import com.tiendadebarrio.roles.dto.RoleUpdateRequest;
import com.tiendadebarrio.roles.entity.Role;
import com.tiendadebarrio.roles.mapper.RoleMapper;
import com.tiendadebarrio.roles.repository.RoleRepository;
import com.tiendadebarrio.users.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String AUDIT_MODULE = "ROLES";
    private static final String AUDIT_ENTITY = "Role";

    private final RoleRepository roleRepository;
    private final AppUserRepository appUserRepository;
    private final RoleMapper roleMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roleRepository.findByDeletedFalseOrderByNameAsc()
                .stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getById(UUID id) {
        return roleMapper.toResponse(findActiveRole(id));
    }

    @Transactional
    public RoleResponse create(RoleCreateRequest request) {
        String name = request.getName().trim().toUpperCase();
        if (roleRepository.existsByNameIgnoreCase(name)) {
            throw new ApiException("Ya existe un rol con ese nombre", HttpStatus.CONFLICT, "ROLE_NAME_TAKEN");
        }

        boolean active = request.getActive() == null || request.getActive();
        Role role = Role.builder()
                .name(name)
                .description(request.getDescription())
                .active(active)
                .build();
        role.setDeleted(false);

        Role saved = roleRepository.save(role);
        auditService.record("CREATE", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), null, snapshot(saved));
        return roleMapper.toResponse(saved);
    }

    @Transactional
    public RoleResponse update(UUID id, RoleUpdateRequest request) {
        Role role = findActiveRole(id);
        Map<String, Object> before = snapshot(role);

        String name = request.getName().trim().toUpperCase();
        if (roleRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ApiException("Ya existe un rol con ese nombre", HttpStatus.CONFLICT, "ROLE_NAME_TAKEN");
        }

        role.setName(name);
        role.setDescription(request.getDescription());

        Role saved = roleRepository.save(role);
        auditService.record("UPDATE", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), before, snapshot(saved));
        return roleMapper.toResponse(saved);
    }

    @Transactional
    public RoleResponse activate(UUID id) {
        Role role = findActiveRole(id);
        if (!role.isActive()) {
            role.setActive(true);
            roleRepository.save(role);
            auditService.record("ACTIVATE", AUDIT_MODULE, AUDIT_ENTITY, role.getId(), null, snapshot(role));
        }
        return roleMapper.toResponse(role);
    }

    @Transactional
    public RoleResponse deactivate(UUID id) {
        Role role = findActiveRole(id);
        if (ADMIN_ROLE.equals(role.getName())) {
            throw new ApiException("No se puede desactivar el rol ADMIN", HttpStatus.CONFLICT, "ADMIN_ROLE_PROTECTED");
        }
        if (appUserRepository.countByRoleIdAndActiveTrueAndDeletedFalse(role.getId()) > 0) {
            throw new ApiException(
                    "No se puede desactivar un rol con usuarios activos asignados",
                    HttpStatus.CONFLICT,
                    "ROLE_HAS_ACTIVE_USERS"
            );
        }
        if (role.isActive()) {
            role.setActive(false);
            roleRepository.save(role);
            auditService.record("DEACTIVATE", AUDIT_MODULE, AUDIT_ENTITY, role.getId(), null, snapshot(role));
        }
        return roleMapper.toResponse(role);
    }

    private Role findActiveRole(UUID id) {
        return roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ApiException("Rol no encontrado", HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND"));
    }

    private Map<String, Object> snapshot(Role role) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", String.valueOf(role.getId()));
        data.put("name", role.getName());
        data.put("description", role.getDescription());
        data.put("active", role.isActive());
        return data;
    }
}
