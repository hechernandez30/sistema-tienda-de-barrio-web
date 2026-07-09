package com.tiendadebarrio.inventory.repository;

import com.tiendadebarrio.inventory.entity.SaleLotAllocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SaleLotAllocationRepository extends JpaRepository<SaleLotAllocation, UUID> {

    List<SaleLotAllocation> findBySaleId(UUID saleId);

    List<SaleLotAllocation> findBySaleItemId(UUID saleItemId);
}
