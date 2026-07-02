package com.tiendadebarrio.inventory.repository;

import com.tiendadebarrio.inventory.entity.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    Page<InventoryMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<InventoryMovement> findByProductIdOrderByCreatedAtDesc(UUID productId);
}
