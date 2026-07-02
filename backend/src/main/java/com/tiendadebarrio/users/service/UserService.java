package com.tiendadebarrio.users.service;

import com.tiendadebarrio.audit.service.AuditService;
import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.roles.entity.Role;
import com.tiendadebarrio.roles.repository.RoleRepository;
import com.tiendadebarrio.security.SecurityUtils;
import com.tiendadebarrio.users.dto.ChangePasswordRequest;
import com.tiendadebarrio.users.dto.UserCreateRequest;
import com.tiendadebarrio.users.dto.UserListResponse;
import com.tiendadebarrio.users.dto.UserResponse;
import com.tiendadebarrio.users.dto.UserUpdateRequest;
import com.tiendadebarrio.users.entity.AppUser;
import com.tiendadebarrio.users.mapper.AppUserMapper;
import com.tiendadebarrio.users.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String AUDIT_MODULE = "USERS";
    private static final String AUDIT_ENTITY = "AppUser";

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUserMapper userMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<UserListResponse> list() {
        return appUserRepository.findByDeletedFalseOrderByUsernameAsc()
                .stream()
                .map(userMapper::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return userMapper.toResponse(findActiveUser(id));
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        Role role = resolveActiveRole(request.getRoleId());

        String username = request.getUsername().trim();
        if (appUserRepository.existsByUsername(username)) {
            throw new ApiException("El nombre de usuario ya está en uso", HttpStatus.CONFLICT, "USERNAME_TAKEN");
        }

        String email = normalizeEmail(request.getEmail());
        if (email != null && appUserRepository.existsByEmail(email)) {
            throw new ApiException("El correo ya está en uso", HttpStatus.CONFLICT, "EMAIL_TAKEN");
        }

        boolean active = request.getActive() == null || request.getActive();
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        AppUser user = AppUser.builder()
                .role(role)
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .active(active)
                .createdBy(currentUserId)
                .build();
        user.setDeleted(false);

        AppUser saved = appUserRepository.save(user);
        auditService.record("CREATE", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), null, snapshot(saved));
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        AppUser user = findActiveUser(id);
        Map<String, Object> before = snapshot(user);

        Role newRole = resolveActiveRole(request.getRoleId());

        String email = normalizeEmail(request.getEmail());
        if (email != null && appUserRepository.existsByEmailAndIdNot(email, id)) {
            throw new ApiException("El correo ya está en uso", HttpStatus.CONFLICT, "EMAIL_TAKEN");
        }

        boolean newActive = request.getActive() == null ? user.isActive() : request.getActive();

        // Reglas al desactivar o al quitar el rol ADMIN mediante actualización.
        boolean wasActiveAdmin = user.isActive() && ADMIN_ROLE.equals(user.getRole().getName());
        boolean willBeActiveAdmin = newActive && ADMIN_ROLE.equals(newRole.getName());
        if (wasActiveAdmin && !willBeActiveAdmin) {
            guardNotSelf(id, "No puedes quitarte a ti mismo el acceso de administrador activo");
            guardNotLastActiveAdmin();
        }
        if (!newActive) {
            guardNotSelf(id, "No puedes desactivar tu propio usuario");
        }

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        user.setRole(newRole);
        user.setEmail(email);
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setActive(newActive);
        user.setUpdatedBy(currentUserId);

        AppUser saved = appUserRepository.save(user);
        auditService.record("UPDATE", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), before, snapshot(saved));
        return userMapper.toResponse(saved);
    }

    @Transactional
    public UserResponse activate(UUID id) {
        AppUser user = findActiveUser(id);
        if (!user.isActive()) {
            user.setActive(true);
            user.setUpdatedBy(SecurityUtils.getCurrentUserId());
            appUserRepository.save(user);
            auditService.record("ACTIVATE", AUDIT_MODULE, AUDIT_ENTITY, user.getId(), null, snapshot(user));
        }
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse deactivate(UUID id) {
        AppUser user = findActiveUser(id);
        guardNotSelf(id, "No puedes desactivar tu propio usuario");
        if (ADMIN_ROLE.equals(user.getRole().getName()) && user.isActive()) {
            guardNotLastActiveAdmin();
        }
        if (user.isActive()) {
            user.setActive(false);
            user.setUpdatedBy(SecurityUtils.getCurrentUserId());
            appUserRepository.save(user);
            auditService.record("DEACTIVATE", AUDIT_MODULE, AUDIT_ENTITY, user.getId(), null, snapshot(user));
        }
        return userMapper.toResponse(user);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        AppUser user = findActiveUser(id);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedBy(SecurityUtils.getCurrentUserId());
        appUserRepository.save(user);
        // No se registra el valor de la contraseña en la bitácora.
        auditService.record("PASSWORD_CHANGE", AUDIT_MODULE, AUDIT_ENTITY, user.getId(), null, identity(user));
    }

    @Transactional
    public void delete(UUID id) {
        AppUser user = findActiveUser(id);
        guardNotSelf(id, "No puedes eliminar tu propio usuario");
        if (ADMIN_ROLE.equals(user.getRole().getName()) && user.isActive()) {
            guardNotLastActiveAdmin();
        }

        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setDeletedBy(SecurityUtils.getCurrentUserId());
        appUserRepository.save(user);
        auditService.record("DELETE", AUDIT_MODULE, AUDIT_ENTITY, user.getId(), null, snapshot(user));
    }

    // ------------------------------------------------------------------
    // Utilidades internas
    // ------------------------------------------------------------------

    private AppUser findActiveUser(UUID id) {
        return appUserRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ApiException("Usuario no encontrado", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
    }

    private Role resolveActiveRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ApiException("El rol indicado no existe", HttpStatus.BAD_REQUEST, "ROLE_NOT_FOUND"));
        if (role.isDeleted()) {
            throw new ApiException("El rol indicado está eliminado", HttpStatus.BAD_REQUEST, "ROLE_DELETED");
        }
        if (!role.isActive()) {
            throw new ApiException("El rol indicado está inactivo", HttpStatus.BAD_REQUEST, "ROLE_INACTIVE");
        }
        return role;
    }

    private void guardNotSelf(UUID targetId, String message) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(targetId)) {
            throw new ApiException(message, HttpStatus.CONFLICT, "SELF_OPERATION_NOT_ALLOWED");
        }
    }

    private void guardNotLastActiveAdmin() {
        if (appUserRepository.countActiveAdmins() <= 1) {
            throw new ApiException(
                    "No se puede desactivar o eliminar al último administrador activo",
                    HttpStatus.CONFLICT,
                    "LAST_ACTIVE_ADMIN"
            );
        }
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private Map<String, Object> snapshot(AppUser user) {
        Map<String, Object> data = identity(user);
        data.put("email", user.getEmail());
        data.put("firstName", user.getFirstName());
        data.put("lastName", user.getLastName());
        data.put("role", user.getRole() != null ? user.getRole().getName() : null);
        data.put("active", user.isActive());
        data.put("deleted", user.isDeleted());
        return data;
    }

    private Map<String, Object> identity(AppUser user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", String.valueOf(user.getId()));
        data.put("username", user.getUsername());
        return data;
    }
}
