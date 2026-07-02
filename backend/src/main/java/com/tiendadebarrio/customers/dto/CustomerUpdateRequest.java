package com.tiendadebarrio.customers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerUpdateRequest {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 160, message = "El nombre completo no puede exceder 160 caracteres")
    private String fullName;

    @Size(max = 30, message = "El NIT no puede exceder 30 caracteres")
    private String nit;

    @Size(max = 30, message = "El teléfono no puede exceder 30 caracteres")
    private String phone;

    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 120, message = "El correo no puede exceder 120 caracteres")
    private String email;

    private String address;

    private Boolean isActive;
}
