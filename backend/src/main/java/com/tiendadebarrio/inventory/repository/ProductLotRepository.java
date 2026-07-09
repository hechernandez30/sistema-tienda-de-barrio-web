package com.tiendadebarrio.inventory.repository;

import com.tiendadebarrio.inventory.entity.ProductLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductLotRepository extends JpaRepository<ProductLot, UUID> {

    List<ProductLot> findByProductIdAndDeletedFalseOrderByExpirationDateAsc(UUID productId);

    @Query("""
            SELECT COALESCE(SUM(l.quantity), 0)
            FROM ProductLot l
            WHERE l.product.id = :productId
              AND l.deleted = false
            """)
    BigDecimal sumQuantityByProduct(@Param("productId") UUID productId);

    @Query("""
            SELECT COALESCE(SUM(l.quantity), 0)
            FROM ProductLot l
            WHERE l.product.id = :productId
              AND l.deleted = false
              AND l.quantity > 0
              AND l.expirationDate >= :today
            """)
    BigDecimal sumSellableQuantity(@Param("productId") UUID productId, @Param("today") LocalDate today);

    @Query("""
            SELECT l FROM ProductLot l
            WHERE l.product.id = :productId
              AND l.deleted = false
              AND l.quantity > 0
              AND l.expirationDate >= :today
            ORDER BY l.expirationDate ASC, l.receivedAt ASC
            """)
    List<ProductLot> findSellableLotsFefo(@Param("productId") UUID productId, @Param("today") LocalDate today);

    Optional<ProductLot> findByPurchaseItemIdAndDeletedFalse(UUID purchaseItemId);

    @Query("""
            SELECT l FROM ProductLot l
            JOIN FETCH l.product p
            WHERE l.deleted = false
              AND l.quantity > 0
              AND l.expirationDate >= :today
              AND l.expirationDate <= :until
              AND p.deleted = false
            ORDER BY l.expirationDate ASC, p.name ASC
            """)
    List<ProductLot> findExpiringBetween(@Param("today") LocalDate today, @Param("until") LocalDate until);

    @Query("""
            SELECT l FROM ProductLot l
            JOIN FETCH l.product p
            WHERE l.deleted = false
              AND l.quantity > 0
              AND l.expirationDate < :today
              AND p.deleted = false
            ORDER BY l.expirationDate ASC, p.name ASC
            """)
    List<ProductLot> findExpiredWithStock(@Param("today") LocalDate today);
}
