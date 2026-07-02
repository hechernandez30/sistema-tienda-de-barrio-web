package com.tiendadebarrio.common.exception;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ErrorResponse {

    private final LocalDateTime timestamp;
    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final String path;
    private final List<FieldErrorDetail> fieldErrors;

    @Getter
    @Builder
    public static class FieldErrorDetail {
        private final String field;
        private final String message;
    }
}
