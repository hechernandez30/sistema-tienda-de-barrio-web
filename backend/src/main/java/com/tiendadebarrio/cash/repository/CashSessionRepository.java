package com.tiendadebarrio.cash.repository;

import com.tiendadebarrio.cash.entity.CashSession;
import com.tiendadebarrio.cash.entity.CashSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashSessionRepository extends JpaRepository<CashSession, UUID> {

    Optional<CashSession> findByIdAndDeletedFalse(UUID id);

    Optional<CashSession> findFirstByStatusAndDeletedFalseOrderByOpenedAtDesc(CashSessionStatus status);

    boolean existsByStatusAndDeletedFalse(CashSessionStatus status);

    List<CashSession> findByDeletedFalseOrderByOpenedAtDesc();
}
