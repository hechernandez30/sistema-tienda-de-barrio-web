package com.tiendadebarrio.suppliers.repository;

import com.tiendadebarrio.suppliers.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    Optional<Supplier> findByIdAndDeletedFalse(UUID id);

    List<Supplier> findByDeletedFalseOrderByNameAsc();

    @Query("""
            SELECT s FROM Supplier s
            WHERE s.deleted = false
              AND (
                    LOWER(s.name) LIKE LOWER(CONCAT('%', :term, '%'))
                    OR LOWER(s.nit) LIKE LOWER(CONCAT('%', :term, '%'))
                    OR LOWER(s.phone) LIKE LOWER(CONCAT('%', :term, '%'))
                    OR LOWER(s.contactName) LIKE LOWER(CONCAT('%', :term, '%'))
              )
            ORDER BY s.name ASC
            """)
    List<Supplier> search(@Param("term") String term);

    boolean existsByNitAndDeletedFalse(String nit);

    boolean existsByNitAndDeletedFalseAndIdNot(String nit, UUID id);

    boolean existsByNameIgnoreCaseAndActiveTrueAndDeletedFalse(String name);

    boolean existsByNameIgnoreCaseAndActiveTrueAndDeletedFalseAndIdNot(String name, UUID id);
}
