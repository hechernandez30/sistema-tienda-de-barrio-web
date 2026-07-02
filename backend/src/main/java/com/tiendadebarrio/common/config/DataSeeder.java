package com.tiendadebarrio.common.config;

import com.tiendadebarrio.roles.entity.Role;
import com.tiendadebarrio.roles.repository.RoleRepository;
import com.tiendadebarrio.users.entity.AppUser;
import com.tiendadebarrio.users.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final String ADMIN_ROLE_NAME = "ADMIN";
    private static final Map<String, String> BASE_ROLES = Map.of(
            "ADMIN", "Administrador del sistema",
            "CAJERO", "Usuario encargado de ventas y caja",
            "INVENTARIO", "Usuario encargado de productos, compras e inventario",
            "REPORTES", "Usuario con acceso a reportes"
    );
    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin123";
    private static final String PLACEHOLDER_PASSWORD_HASH =
            "$2a$10$replace.this.hash.from.backend.generated.bcrypt";

    private final RoleRepository roleRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureBaseRoles();
        Role adminRole = roleRepository.findByName(ADMIN_ROLE_NAME)
                .orElseThrow(() -> new IllegalStateException("El rol ADMIN no pudo crearse"));
        ensureAdminUser(adminRole);
    }

    private void ensureBaseRoles() {
        BASE_ROLES.forEach((name, description) -> roleRepository.findByName(name).orElseGet(() -> {
            Role role = Role.builder()
                    .name(name)
                    .description(description)
                    .active(true)
                    .build();
            role.setDeleted(false);
            Role saved = roleRepository.save(role);
            log.info("Rol {} creado por el seeder", name);
            return saved;
        }));
    }

    private void ensureAdminUser(Role adminRole) {
        appUserRepository.findByUsername(ADMIN_USERNAME).ifPresentOrElse(
                existingUser -> updatePlaceholderPasswordIfNeeded(existingUser),
                () -> createAdminUser(adminRole)
        );
    }

    private void createAdminUser(Role adminRole) {
        AppUser adminUser = AppUser.builder()
                .role(adminRole)
                .username(ADMIN_USERNAME)
                .email("admin@local.test")
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .firstName("Administrador")
                .lastName("Sistema")
                .active(true)
                .build();
        adminUser.setDeleted(false);
        appUserRepository.save(adminUser);
        log.info("Usuario {} creado por el seeder", ADMIN_USERNAME);
    }

    private void updatePlaceholderPasswordIfNeeded(AppUser existingUser) {
        if (PLACEHOLDER_PASSWORD_HASH.equals(existingUser.getPasswordHash())) {
            existingUser.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            appUserRepository.save(existingUser);
            log.info("Contraseña del usuario {} actualizada desde hash placeholder", ADMIN_USERNAME);
        }
    }
}
