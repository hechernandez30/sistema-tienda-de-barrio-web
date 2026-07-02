package com.tiendadebarrio.cash.service;

import com.tiendadebarrio.audit.service.AuditService;
import com.tiendadebarrio.cash.dto.CashMovementRequest;
import com.tiendadebarrio.cash.dto.CashMovementResponse;
import com.tiendadebarrio.cash.dto.CashSessionResponse;
import com.tiendadebarrio.cash.dto.CashSummaryResponse;
import com.tiendadebarrio.cash.dto.CloseCashSessionRequest;
import com.tiendadebarrio.cash.dto.OpenCashSessionRequest;
import com.tiendadebarrio.cash.entity.CashMovement;
import com.tiendadebarrio.cash.entity.CashMovementType;
import com.tiendadebarrio.cash.entity.CashSession;
import com.tiendadebarrio.cash.entity.CashSessionStatus;
import com.tiendadebarrio.cash.mapper.CashMapper;
import com.tiendadebarrio.cash.repository.CashMovementRepository;
import com.tiendadebarrio.cash.repository.CashSessionRepository;
import com.tiendadebarrio.common.enums.PaymentMethod;
import com.tiendadebarrio.common.exception.ApiException;
import com.tiendadebarrio.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CashService {

    private static final String AUDIT_MODULE = "CASH";
    private static final String CATEGORY_SALE = "VENTA";
    private static final String CATEGORY_SALE_CANCELLATION = "ANULACION_VENTA";
    private static final String CATEGORY_PURCHASE = "COMPRA";
    private static final int MONEY_SCALE = 2;

    private final CashSessionRepository cashSessionRepository;
    private final CashMovementRepository cashMovementRepository;
    private final CashMapper cashMapper;
    private final AuditService auditService;

    // ----------------------------------------------------------------
    // Sesiones de caja
    // ----------------------------------------------------------------

    @Transactional
    public CashSessionResponse openSession(OpenCashSessionRequest request) {
        if (cashSessionRepository.existsByStatusAndDeletedFalse(CashSessionStatus.OPEN)) {
            throw new ApiException(
                    "Ya existe una caja abierta. Debe cerrarla antes de abrir otra",
                    HttpStatus.CONFLICT,
                    "CASH_SESSION_ALREADY_OPEN"
            );
        }

        BigDecimal openingAmount = scaled(request.getOpeningAmount());
        UUID currentUserId = SecurityUtils.getCurrentUserId();

        CashSession session = CashSession.builder()
                .openedBy(currentUserId)
                .openedAt(LocalDateTime.now())
                .openingAmount(openingAmount)
                .expectedAmount(openingAmount)
                .status(CashSessionStatus.OPEN)
                .notes(request.getNotes())
                .build();
        session.setDeleted(false);

        CashSession saved = cashSessionRepository.save(session);

        auditService.record("OPEN", AUDIT_MODULE, "CashSession", saved.getId(), null, sessionSnapshot(saved));

        return cashMapper.toSessionResponse(saved);
    }

    @Transactional
    public CashSessionResponse closeSession(UUID id, CloseCashSessionRequest request) {
        CashSession session = cashSessionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(this::sessionNotFound);

        if (session.getStatus() != CashSessionStatus.OPEN) {
            throw new ApiException(
                    "Solo se puede cerrar una caja que esté abierta",
                    HttpStatus.CONFLICT,
                    "CASH_SESSION_NOT_OPEN"
            );
        }

        BigDecimal expected = recalculateExpected(session);
        BigDecimal counted = scaled(request.getCountedAmount());

        session.setExpectedAmount(expected);
        session.setCountedAmount(counted);
        session.setDifferenceAmount(counted.subtract(expected).setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        session.setStatus(CashSessionStatus.CLOSED);
        session.setClosedBy(SecurityUtils.getCurrentUserId());
        session.setClosedAt(LocalDateTime.now());
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            session.setNotes(request.getNotes());
        }

        CashSession saved = cashSessionRepository.save(session);

        auditService.record("CLOSE", AUDIT_MODULE, "CashSession", saved.getId(), null, sessionSnapshot(saved));

        return cashMapper.toSessionResponse(saved);
    }

    @Transactional(readOnly = true)
    public CashSummaryResponse getCurrentSummary() {
        CashSession session = getOpenSession()
                .orElseThrow(() -> new ApiException(
                        "No hay una caja abierta actualmente",
                        HttpStatus.NOT_FOUND,
                        "NO_OPEN_CASH_SESSION"
                ));
        return buildSummary(session);
    }

    @Transactional(readOnly = true)
    public List<CashSessionResponse> listSessions() {
        return cashSessionRepository.findByDeletedFalseOrderByOpenedAtDesc()
                .stream()
                .map(cashMapper::toSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CashSessionResponse getSession(UUID id) {
        CashSession session = cashSessionRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(this::sessionNotFound);
        return cashMapper.toSessionResponse(session);
    }

    @Transactional(readOnly = true)
    public List<CashMovementResponse> getSessionMovements(UUID sessionId) {
        cashSessionRepository.findByIdAndDeletedFalse(sessionId)
                .orElseThrow(this::sessionNotFound);
        return cashMovementRepository.findByCashSessionIdAndDeletedFalseOrderByCreatedAtDesc(sessionId)
                .stream()
                .map(cashMapper::toMovementResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CashMovementResponse> listMovements(int page, int size) {
        return cashMovementRepository
                .findByDeletedFalseOrderByCreatedAtDesc(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(cashMapper::toMovementResponse)
                .toList();
    }

    // ----------------------------------------------------------------
    // Movimientos manuales
    // ----------------------------------------------------------------

    @Transactional
    public CashMovementResponse registerManualMovement(CashMovementRequest request) {
        CashSession session = resolveTargetSession(request.getCashSessionId());

        CashMovement movement = applyMovement(
                session,
                request.getMovementType(),
                request.getCategory(),
                request.getPaymentMethod(),
                request.getAmount(),
                request.getDescription(),
                null,
                null,
                SecurityUtils.getCurrentUserId());

        auditService.record(
                "MOVEMENT_" + request.getMovementType().name(),
                AUDIT_MODULE,
                "CashMovement",
                movement.getId(),
                null,
                movementSnapshot(movement));

        return cashMapper.toMovementResponse(movement);
    }

    // ----------------------------------------------------------------
    // Integraciones futuras (ventas y compras)
    // ----------------------------------------------------------------

    /**
     * Punto de integración para el módulo de ventas: registra automáticamente un ingreso
     * en la caja abierta cuando se confirma una venta.
     */
    @Transactional
    public CashMovement registerSaleIncome(UUID saleId, BigDecimal amount, PaymentMethod paymentMethod, UUID userId) {
        CashSession session = requireOpenSession();
        return applyMovement(
                session,
                CashMovementType.INCOME,
                CATEGORY_SALE,
                paymentMethod,
                amount,
                "Ingreso por venta",
                saleId,
                null,
                userId);
    }

    /**
     * Punto de integración para el módulo de ventas: registra automáticamente un egreso
     * en la caja abierta cuando se anula una venta.
     */
    @Transactional
    public CashMovement registerSaleCancellationExpense(UUID saleId, BigDecimal amount, PaymentMethod paymentMethod, UUID userId) {
        CashSession session = requireOpenSession();
        return applyMovement(
                session,
                CashMovementType.EXPENSE,
                CATEGORY_SALE_CANCELLATION,
                paymentMethod,
                amount,
                "Egreso por anulación de venta",
                saleId,
                null,
                userId);
    }

    /**
     * Devuelve el id de la caja abierta actual o lanza excepción si no existe.
     * Útil para que el módulo de ventas valide y asocie la sesión de caja a la venta.
     */
    @Transactional(readOnly = true)
    public UUID getCurrentOpenSessionId() {
        return requireOpenSession().getId();
    }

    /**
     * Punto de integración para el módulo de compras: registra automáticamente un egreso
     * en la caja abierta cuando se confirma una compra pagada.
     */
    @Transactional
    public CashMovement registerPurchaseExpense(UUID purchaseId, BigDecimal amount, PaymentMethod paymentMethod, UUID userId) {
        CashSession session = requireOpenSession();
        return applyMovement(
                session,
                CashMovementType.EXPENSE,
                CATEGORY_PURCHASE,
                paymentMethod,
                amount,
                "Egreso por compra",
                null,
                purchaseId,
                userId);
    }

    // ----------------------------------------------------------------
    // Núcleo compartido
    // ----------------------------------------------------------------

    private CashMovement applyMovement(
            CashSession session,
            CashMovementType type,
            String category,
            PaymentMethod paymentMethod,
            BigDecimal amount,
            String description,
            UUID referenceSaleId,
            UUID referencePurchaseId,
            UUID userId) {
        if (amount == null || amount.signum() <= 0) {
            throw new ApiException(
                    "El monto debe ser mayor que cero",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_AMOUNT"
            );
        }

        BigDecimal scaledAmount = scaled(amount);

        CashMovement movement = CashMovement.builder()
                .cashSessionId(session.getId())
                .movementType(type)
                .category(category)
                .paymentMethod(paymentMethod)
                .amount(scaledAmount)
                .description(description)
                .referenceSaleId(referenceSaleId)
                .referencePurchaseId(referencePurchaseId)
                .createdBy(userId)
                .deleted(false)
                .build();

        BigDecimal expected = type == CashMovementType.INCOME
                ? session.getExpectedAmount().add(scaledAmount)
                : session.getExpectedAmount().subtract(scaledAmount);
        session.setExpectedAmount(expected.setScale(MONEY_SCALE, RoundingMode.HALF_UP));

        cashSessionRepository.save(session);
        return cashMovementRepository.save(movement);
    }

    private CashSession resolveTargetSession(UUID cashSessionId) {
        if (cashSessionId == null) {
            return requireOpenSession();
        }
        CashSession session = cashSessionRepository.findByIdAndDeletedFalse(cashSessionId)
                .orElseThrow(this::sessionNotFound);
        if (session.getStatus() != CashSessionStatus.OPEN) {
            throw new ApiException(
                    "No se pueden registrar movimientos en una caja cerrada",
                    HttpStatus.CONFLICT,
                    "CASH_SESSION_CLOSED"
            );
        }
        return session;
    }

    private CashSession requireOpenSession() {
        return getOpenSession()
                .orElseThrow(() -> new ApiException(
                        "No hay una caja abierta para registrar el movimiento",
                        HttpStatus.CONFLICT,
                        "NO_OPEN_CASH_SESSION"
                ));
    }

    private java.util.Optional<CashSession> getOpenSession() {
        return cashSessionRepository.findFirstByStatusAndDeletedFalseOrderByOpenedAtDesc(CashSessionStatus.OPEN);
    }

    private BigDecimal recalculateExpected(CashSession session) {
        List<CashMovement> movements =
                cashMovementRepository.findByCashSessionIdAndDeletedFalseOrderByCreatedAtDesc(session.getId());

        BigDecimal incomes = movements.stream()
                .filter(m -> m.getMovementType() == CashMovementType.INCOME)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expenses = movements.stream()
                .filter(m -> m.getMovementType() == CashMovementType.EXPENSE)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return session.getOpeningAmount().add(incomes).subtract(expenses).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private CashSummaryResponse buildSummary(CashSession session) {
        List<CashMovement> movements =
                cashMovementRepository.findByCashSessionIdAndDeletedFalseOrderByCreatedAtDesc(session.getId());

        BigDecimal totalCashIncome = sumIncome(movements, PaymentMethod.CASH);
        BigDecimal totalTransferIncome = sumIncome(movements, PaymentMethod.TRANSFER);
        BigDecimal totalCardIncome = sumIncome(movements, PaymentMethod.CARD);
        BigDecimal totalExpenses = movements.stream()
                .filter(m -> m.getMovementType() == CashMovementType.EXPENSE)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        return CashSummaryResponse.builder()
                .cashSessionId(session.getId())
                .status(session.getStatus())
                .openedAt(session.getOpenedAt())
                .openedBy(session.getOpenedBy())
                .openingAmount(session.getOpeningAmount())
                .totalCashIncome(totalCashIncome)
                .totalTransferIncome(totalTransferIncome)
                .totalCardIncome(totalCardIncome)
                .totalExpenses(totalExpenses)
                .expectedAmount(session.getExpectedAmount())
                .movementCount(movements.size())
                .build();
    }

    private BigDecimal sumIncome(List<CashMovement> movements, PaymentMethod paymentMethod) {
        return movements.stream()
                .filter(m -> m.getMovementType() == CashMovementType.INCOME)
                .filter(m -> m.getPaymentMethod() == paymentMethod)
                .map(CashMovement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scaled(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private ApiException sessionNotFound() {
        return new ApiException("Sesión de caja no encontrada", HttpStatus.NOT_FOUND, "CASH_SESSION_NOT_FOUND");
    }

    private Map<String, Object> sessionSnapshot(CashSession session) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", String.valueOf(session.getId()));
        snapshot.put("status", String.valueOf(session.getStatus()));
        snapshot.put("openingAmount", String.valueOf(session.getOpeningAmount()));
        snapshot.put("expectedAmount", String.valueOf(session.getExpectedAmount()));
        snapshot.put("countedAmount", String.valueOf(session.getCountedAmount()));
        snapshot.put("differenceAmount", String.valueOf(session.getDifferenceAmount()));
        return snapshot;
    }

    private Map<String, Object> movementSnapshot(CashMovement movement) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", String.valueOf(movement.getId()));
        snapshot.put("cashSessionId", String.valueOf(movement.getCashSessionId()));
        snapshot.put("movementType", String.valueOf(movement.getMovementType()));
        snapshot.put("category", String.valueOf(movement.getCategory()));
        snapshot.put("paymentMethod", String.valueOf(movement.getPaymentMethod()));
        snapshot.put("amount", String.valueOf(movement.getAmount()));
        return snapshot;
    }
}
