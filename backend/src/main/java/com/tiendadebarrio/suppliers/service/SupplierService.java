package com.tiendadebarrio.suppliers.service;

import com.tiendadebarrio.audit.service.AuditService;
import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.security.SecurityUtils;
import com.tiendadebarrio.suppliers.dto.SupplierCreateRequest;
import com.tiendadebarrio.suppliers.dto.SupplierListResponse;
import com.tiendadebarrio.suppliers.dto.SupplierResponse;
import com.tiendadebarrio.suppliers.dto.SupplierUpdateRequest;
import com.tiendadebarrio.suppliers.entity.Supplier;
import com.tiendadebarrio.suppliers.mapper.SupplierMapper;
import com.tiendadebarrio.suppliers.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private static final String AUDIT_MODULE = "SUPPLIERS";
    private static final String AUDIT_ENTITY = "Supplier";

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<SupplierListResponse> list() {
        return supplierRepository.findByDeletedFalseOrderByNameAsc()
                .stream()
                .map(supplierMapper::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse getById(UUID id) {
        return supplierMapper.toResponse(findActiveSupplier(id));
    }

    @Transactional(readOnly = true)
    public List<SupplierListResponse> search(String term) {
        if (!StringUtils.hasText(term)) {
            return List.of();
        }
        return supplierRepository.search(term.trim())
                .stream()
                .map(supplierMapper::toListResponse)
                .toList();
    }

    @Transactional
    public SupplierResponse create(SupplierCreateRequest request) {
        String name = request.getName().trim();
        String nit = normalize(request.getNit());

        validateNameUniqueness(name, null);
        validateNitUniqueness(nit, null);

        UUID currentUserId = SecurityUtils.getCurrentUserId();

        Supplier supplier = Supplier.builder()
                .name(name)
                .nit(nit)
                .contactName(normalize(request.getContactName()))
                .phone(normalize(request.getPhone()))
                .email(normalize(request.getEmail()))
                .address(request.getAddress())
                .active(request.getIsActive() == null || request.getIsActive())
                .createdBy(currentUserId)
                .build();
        supplier.setDeleted(false);

        Supplier saved = supplierRepository.save(supplier);

        auditService.record("CREATE", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), null, auditSnapshot(saved));

        return supplierMapper.toResponse(saved);
    }

    @Transactional
    public SupplierResponse update(UUID id, SupplierUpdateRequest request) {
        Supplier supplier = findActiveSupplier(id);
        Map<String, Object> previousSnapshot = auditSnapshot(supplier);

        String name = request.getName().trim();
        String nit = normalize(request.getNit());
        boolean active = request.getIsActive() == null ? supplier.isActive() : request.getIsActive();

        validateNameUniqueness(name, id, active);
        validateNitUniqueness(nit, id);

        supplier.setName(name);
        supplier.setNit(nit);
        supplier.setContactName(normalize(request.getContactName()));
        supplier.setPhone(normalize(request.getPhone()));
        supplier.setEmail(normalize(request.getEmail()));
        supplier.setAddress(request.getAddress());
        supplier.setActive(active);
        supplier.setUpdatedBy(SecurityUtils.getCurrentUserId());

        Supplier saved = supplierRepository.save(supplier);

        auditService.record("UPDATE", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), previousSnapshot, auditSnapshot(saved));

        return supplierMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Supplier supplier = findActiveSupplier(id);
        Map<String, Object> previousSnapshot = auditSnapshot(supplier);

        supplier.setDeleted(true);
        supplier.setDeletedAt(LocalDateTime.now());
        supplier.setDeletedBy(SecurityUtils.getCurrentUserId());
        supplier.setActive(false);

        supplierRepository.save(supplier);

        auditService.record("DELETE", AUDIT_MODULE, AUDIT_ENTITY, supplier.getId(), previousSnapshot, null);
    }

    private Supplier findActiveSupplier(UUID id) {
        return supplierRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ApiException(
                        "Proveedor no encontrado",
                        HttpStatus.NOT_FOUND,
                        "SUPPLIER_NOT_FOUND"
                ));
    }

    private void validateNameUniqueness(String name, UUID excludeId) {
        validateNameUniqueness(name, excludeId, true);
    }

    private void validateNameUniqueness(String name, UUID excludeId, boolean active) {
        if (!active) {
            return;
        }
        boolean exists = excludeId == null
                ? supplierRepository.existsByNameIgnoreCaseAndActiveTrueAndDeletedFalse(name)
                : supplierRepository.existsByNameIgnoreCaseAndActiveTrueAndDeletedFalseAndIdNot(name, excludeId);
        if (exists) {
            throw new ApiException(
                    "Ya existe un proveedor activo con el nombre " + name,
                    HttpStatus.CONFLICT,
                    "SUPPLIER_NAME_DUPLICATED"
            );
        }
    }

    private void validateNitUniqueness(String nit, UUID excludeId) {
        if (nit == null) {
            return;
        }
        boolean exists = excludeId == null
                ? supplierRepository.existsByNitAndDeletedFalse(nit)
                : supplierRepository.existsByNitAndDeletedFalseAndIdNot(nit, excludeId);
        if (exists) {
            throw new ApiException(
                    "Ya existe un proveedor con el NIT " + nit,
                    HttpStatus.CONFLICT,
                    "SUPPLIER_NIT_DUPLICATED"
            );
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Map<String, Object> auditSnapshot(Supplier supplier) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", String.valueOf(supplier.getId()));
        snapshot.put("name", String.valueOf(supplier.getName()));
        snapshot.put("nit", String.valueOf(supplier.getNit()));
        snapshot.put("contactName", String.valueOf(supplier.getContactName()));
        snapshot.put("phone", String.valueOf(supplier.getPhone()));
        snapshot.put("email", String.valueOf(supplier.getEmail()));
        snapshot.put("active", String.valueOf(supplier.isActive()));
        return snapshot;
    }
}
