package com.tiendadebarrio.cash.mapper;

import com.tiendadebarrio.cash.dto.CashMovementResponse;
import com.tiendadebarrio.cash.dto.CashSessionResponse;
import com.tiendadebarrio.cash.entity.CashMovement;
import com.tiendadebarrio.cash.entity.CashSession;
import org.springframework.stereotype.Component;

@Component
public class CashMapper {

    public CashSessionResponse toSessionResponse(CashSession session) {
        return CashSessionResponse.builder()
                .id(session.getId())
                .openedBy(session.getOpenedBy())
                .closedBy(session.getClosedBy())
                .openedAt(session.getOpenedAt())
                .closedAt(session.getClosedAt())
                .openingAmount(session.getOpeningAmount())
                .expectedAmount(session.getExpectedAmount())
                .countedAmount(session.getCountedAmount())
                .differenceAmount(session.getDifferenceAmount())
                .status(session.getStatus())
                .notes(session.getNotes())
                .build();
    }

    public CashMovementResponse toMovementResponse(CashMovement movement) {
        return CashMovementResponse.builder()
                .id(movement.getId())
                .cashSessionId(movement.getCashSessionId())
                .movementType(movement.getMovementType())
                .category(movement.getCategory())
                .paymentMethod(movement.getPaymentMethod())
                .amount(movement.getAmount())
                .description(movement.getDescription())
                .referenceSaleId(movement.getReferenceSaleId())
                .referencePurchaseId(movement.getReferencePurchaseId())
                .createdAt(movement.getCreatedAt())
                .createdBy(movement.getCreatedBy())
                .build();
    }
}
