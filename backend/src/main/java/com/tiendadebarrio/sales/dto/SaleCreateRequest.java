package com.tiendadebarrio.sales.dto;

import com.tiendadebarrio.common.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class SaleCreateRequest {

    private UUID customerId;

    private PaymentMethod paymentMethod;

    private String notes;

    @NotEmpty(message = "La venta debe incluir al menos un producto")
    @Valid
    private List<SaleItemRequest> items;
}
