package com.tiendadebarrio.customers.service;

import com.tiendadebarrio.audit.service.AuditService;
import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.customers.dto.CustomerCreateRequest;
import com.tiendadebarrio.customers.dto.CustomerListResponse;
import com.tiendadebarrio.customers.dto.CustomerResponse;
import com.tiendadebarrio.customers.dto.CustomerUpdateRequest;
import com.tiendadebarrio.customers.entity.Customer;
import com.tiendadebarrio.customers.mapper.CustomerMapper;
import com.tiendadebarrio.customers.repository.CustomerRepository;
import com.tiendadebarrio.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private static final String AUDIT_MODULE = "CUSTOMERS";
    private static final String AUDIT_ENTITY = "Customer";

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<CustomerListResponse> list() {
        return customerRepository.findByDeletedFalseOrderByFullNameAsc()
                .stream()
                .map(customerMapper::toListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        return customerMapper.toResponse(findActiveCustomer(id));
    }

    @Transactional(readOnly = true)
    public List<CustomerListResponse> search(String term) {
        if (!StringUtils.hasText(term)) {
            return List.of();
        }
        return customerRepository.search(term.trim())
                .stream()
                .map(customerMapper::toListResponse)
                .toList();
    }

    @Transactional
    public CustomerResponse create(CustomerCreateRequest request) {
        String fullName = request.getFullName().trim();
        String nit = normalize(request.getNit());

        validateNitUniqueness(nit, null);

        UUID currentUserId = SecurityUtils.getCurrentUserId();

        Customer customer = Customer.builder()
                .fullName(fullName)
                .nit(nit)
                .phone(normalize(request.getPhone()))
                .email(normalize(request.getEmail()))
                .address(request.getAddress())
                .active(request.getIsActive() == null || request.getIsActive())
                .createdBy(currentUserId)
                .build();
        customer.setDeleted(false);

        Customer saved = customerRepository.save(customer);

        auditService.record("CREATE", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), null, auditSnapshot(saved));

        return customerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse update(UUID id, CustomerUpdateRequest request) {
        Customer customer = findActiveCustomer(id);
        Map<String, Object> previousSnapshot = auditSnapshot(customer);

        String fullName = request.getFullName().trim();
        String nit = normalize(request.getNit());

        validateNitUniqueness(nit, id);

        customer.setFullName(fullName);
        customer.setNit(nit);
        customer.setPhone(normalize(request.getPhone()));
        customer.setEmail(normalize(request.getEmail()));
        customer.setAddress(request.getAddress());
        if (request.getIsActive() != null) {
            customer.setActive(request.getIsActive());
        }
        customer.setUpdatedBy(SecurityUtils.getCurrentUserId());

        Customer saved = customerRepository.save(customer);

        auditService.record("UPDATE", AUDIT_MODULE, AUDIT_ENTITY, saved.getId(), previousSnapshot, auditSnapshot(saved));

        return customerMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Customer customer = findActiveCustomer(id);
        Map<String, Object> previousSnapshot = auditSnapshot(customer);

        customer.setDeleted(true);
        customer.setDeletedAt(LocalDateTime.now());
        customer.setDeletedBy(SecurityUtils.getCurrentUserId());
        customer.setActive(false);

        customerRepository.save(customer);

        auditService.record("DELETE", AUDIT_MODULE, AUDIT_ENTITY, customer.getId(), previousSnapshot, null);
    }

    private Customer findActiveCustomer(UUID id) {
        return customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ApiException(
                        "Cliente no encontrado",
                        HttpStatus.NOT_FOUND,
                        "CUSTOMER_NOT_FOUND"
                ));
    }

    private void validateNitUniqueness(String nit, UUID excludeId) {
        if (nit == null) {
            return;
        }
        boolean exists = excludeId == null
                ? customerRepository.existsByNitAndDeletedFalse(nit)
                : customerRepository.existsByNitAndDeletedFalseAndIdNot(nit, excludeId);
        if (exists) {
            throw new ApiException(
                    "Ya existe un cliente con el NIT " + nit,
                    HttpStatus.CONFLICT,
                    "CUSTOMER_NIT_DUPLICATED"
            );
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Map<String, Object> auditSnapshot(Customer customer) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", String.valueOf(customer.getId()));
        snapshot.put("fullName", String.valueOf(customer.getFullName()));
        snapshot.put("nit", String.valueOf(customer.getNit()));
        snapshot.put("phone", String.valueOf(customer.getPhone()));
        snapshot.put("email", String.valueOf(customer.getEmail()));
        snapshot.put("active", String.valueOf(customer.isActive()));
        return snapshot;
    }
}
