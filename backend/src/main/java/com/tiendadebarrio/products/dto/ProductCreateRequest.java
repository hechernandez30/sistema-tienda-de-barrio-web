package com.tiendadebarrio.products.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class ProductCreateRequest {

    @NotBlank(message = "El código de barras es obligatorio")
    @Size(max = 80, message = "El código de barras no puede exceder 80 caracteres")
    private String barcode;

    @Size(max = 80, message = "El SKU no puede exceder 80 caracteres")
    private String sku;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 160, message = "El nombre no puede exceder 160 caracteres")
    private String name;

    private String description;

    private UUID categoryId;

    private UUID unitMeasureId;

    @DecimalMin(value = "0.0", message = "El precio de compra no puede ser negativo")
    private BigDecimal purchasePrice;

    @DecimalMin(value = "0.0", message = "El precio de venta no puede ser negativo")
    private BigDecimal salePrice;

    @DecimalMin(value = "0.0", message = "El stock mínimo no puede ser negativo")
    private BigDecimal minStock;

    @DecimalMin(value = "0.0", message = "El stock actual no puede ser negativo")
    private BigDecimal currentStock;

    private Boolean active;
}
