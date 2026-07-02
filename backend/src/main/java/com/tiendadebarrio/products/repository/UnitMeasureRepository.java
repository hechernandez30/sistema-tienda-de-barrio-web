package com.tiendadebarrio.products.repository;

import com.tiendadebarrio.products.entity.UnitMeasure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitMeasureRepository extends JpaRepository<UnitMeasure, UUID> {

    Optional<UnitMeasure> findByIdAndDeletedFalse(UUID id);

    List<UnitMeasure> findByDeletedFalseAndActiveTrueOrderByNameAsc();

    boolean existsByCodeIgnoreCaseAndDeletedFalse(String code);
}
