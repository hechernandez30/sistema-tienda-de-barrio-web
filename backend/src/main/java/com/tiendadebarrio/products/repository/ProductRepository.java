package com.tiendadebarrio.products.repository;

import com.tiendadebarrio.products.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByDeletedFalseOrderByNameAsc();

    @Query("""
            SELECT p FROM Product p
            WHERE p.deleted = false
              AND p.active = true
              AND p.currentStock <= p.minStock
            ORDER BY p.name ASC
            """)
    List<Product> findLowStock();

    Optional<Product> findByIdAndDeletedFalse(UUID id);

    Optional<Product> findByBarcodeAndDeletedFalseAndActiveTrue(String barcode);

    List<Product> findByDeletedFalseAndActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(
            String name,
            Pageable pageable);

    boolean existsByBarcodeAndDeletedFalse(String barcode);

    boolean existsByBarcodeAndDeletedFalseAndIdNot(String barcode, UUID id);

    boolean existsBySkuAndDeletedFalse(String sku);

    boolean existsBySkuAndDeletedFalseAndIdNot(String sku, UUID id);
}
