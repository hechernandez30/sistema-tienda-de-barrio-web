package com.tiendadebarrio.customers.repository;

import com.tiendadebarrio.customers.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndDeletedFalse(UUID id);

    List<Customer> findByDeletedFalseOrderByFullNameAsc();

    @Query("""
            SELECT c FROM Customer c
            WHERE c.deleted = false
              AND (
                    LOWER(c.fullName) LIKE LOWER(CONCAT('%', :term, '%'))
                    OR LOWER(c.nit) LIKE LOWER(CONCAT('%', :term, '%'))
                    OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :term, '%'))
                    OR LOWER(c.email) LIKE LOWER(CONCAT('%', :term, '%'))
              )
            ORDER BY c.fullName ASC
            """)
    List<Customer> search(@Param("term") String term);

    boolean existsByNitAndDeletedFalse(String nit);

    boolean existsByNitAndDeletedFalseAndIdNot(String nit, UUID id);
}
