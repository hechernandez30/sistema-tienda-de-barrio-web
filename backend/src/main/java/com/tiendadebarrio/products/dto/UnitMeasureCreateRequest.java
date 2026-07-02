package com.tiendadebarrio.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnitMeasureCreateRequest {

    @NotBlank(message = "El código de la unidad es obligatorio")
    @Size(max = 20, message = "El código no puede exceder 20 caracteres")
    private String code;

    @NotBlank(message = "El nombre de la unidad es obligatorio")
    @Size(max = 80, message = "El nombre no puede exceder 80 caracteres")
    private String name;
}
