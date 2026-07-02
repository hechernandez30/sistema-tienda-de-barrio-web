package com.tiendadebarrio.purchases.repository;

import com.tiendadebarrio.purchases.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    @Query("""
            SELECT p FROM Purchase p
            LEFT JOIN FETCH p.supplier
            WHERE p.deleted = false
            ORDER BY p.createdAt DESC
            """)
    List<Purchase> findListView();

    @Query("""
            SELECT DISTINCT p FROM Purchase p
            LEFT JOIN FETCH p.supplier
            LEFT JOIN FETCH p.items i
            LEFT JOIN FETCH i.product
            WHERE p.id = :id AND p.deleted = false
            """)
    Optional<Purchase> findDetailById(@Param("id") UUID id);
}
