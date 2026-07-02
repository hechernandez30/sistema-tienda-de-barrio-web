package com.tiendadebarrio.cash.repository;

import com.tiendadebarrio.cash.entity.CashMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CashMovementRepository extends JpaRepository<CashMovement, UUID> {

    List<CashMovement> findByCashSessionIdAndDeletedFalseOrderByCreatedAtDesc(UUID cashSessionId);

    Page<CashMovement> findByDeletedFalseOrderByCreatedAtDesc(Pageable pageable);
}
