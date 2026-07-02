package com.tiendadebarrio.users.repository;

import com.tiendadebarrio.users.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    List<AppUser> findByDeletedFalseOrderByUsernameAsc();

    Optional<AppUser> findByIdAndDeletedFalse(UUID id);

    long countByRoleIdAndActiveTrueAndDeletedFalse(UUID roleId);

    @Query("""
            SELECT COUNT(u) FROM AppUser u
            WHERE u.deleted = false
              AND u.active = true
              AND u.role.name = 'ADMIN'
            """)
    long countActiveAdmins();
}
