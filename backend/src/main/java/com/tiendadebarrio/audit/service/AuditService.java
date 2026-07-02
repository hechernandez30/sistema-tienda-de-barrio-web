package com.tiendadebarrio.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiendadebarrio.audit.entity.AuditLog;
import com.tiendadebarrio.audit.repository.AuditLogRepository;
import com.tiendadebarrio.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public void record(
            String action,
            String module,
            String entityName,
            UUID entityId,
            Object oldValue,
            Object newValue) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(SecurityUtils.getCurrentUserId())
                    .username(SecurityUtils.getCurrentUsername())
                    .action(action)
                    .module(module)
                    .entityName(entityName)
                    .entityId(entityId)
                    .oldValue(toJson(oldValue))
                    .newValue(toJson(newValue))
                    .ipAddress(resolveIpAddress())
                    .userAgent(resolveUserAgent())
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.warn("No fue posible registrar la bitácora para {} {}: {}", module, action, ex.getMessage());
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            log.warn("No fue posible serializar el valor para bitácora: {}", ex.getMessage());
            return null;
        }
    }

    private String resolveIpAddress() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getRemoteAddr() : null;
    }

    private String resolveUserAgent() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getHeader("User-Agent") : null;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
