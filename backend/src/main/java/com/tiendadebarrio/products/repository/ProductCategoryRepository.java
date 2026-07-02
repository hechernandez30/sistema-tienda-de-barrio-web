package com.tiendadebarrio.products.repository;

import com.tiendadebarrio.products.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {

    Optional<ProductCategory> findByIdAndDeletedFalse(UUID id);

    List<ProductCategory> findByDeletedFalseAndActiveTrueOrderByNameAsc();

    boolean existsByNameIgnoreCaseAndDeletedFalse(String name);
}
