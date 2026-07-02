package com.tiendadebarrio.roles.repository;

import com.tiendadebarrio.roles.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    List<Role> findByDeletedFalseOrderByNameAsc();

    Optional<Role> findByIdAndDeletedFalse(UUID id);
}
