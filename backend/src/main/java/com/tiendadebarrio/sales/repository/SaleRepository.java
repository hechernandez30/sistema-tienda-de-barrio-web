package com.tiendadebarrio.sales.repository;

import com.tiendadebarrio.sales.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    @Query("""
            SELECT s FROM Sale s
            LEFT JOIN FETCH s.customer
            LEFT JOIN FETCH s.cashier
            WHERE s.deleted = false
            ORDER BY s.saleDate DESC
            """)
    List<Sale> findListView();

    @Query("""
            SELECT DISTINCT s FROM Sale s
            LEFT JOIN FETCH s.customer
            LEFT JOIN FETCH s.cashier
            LEFT JOIN FETCH s.items i
            LEFT JOIN FETCH i.product
            WHERE s.id = :id AND s.deleted = false
            """)
    Optional<Sale> findDetailById(@Param("id") UUID id);

    @Query("""
            SELECT s FROM Sale s
            LEFT JOIN FETCH s.customer
            LEFT JOIN FETCH s.cashier
            WHERE s.deleted = false
              AND s.saleDate >= :start
              AND s.saleDate < :end
            ORDER BY s.saleDate DESC
            """)
    List<Sale> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
