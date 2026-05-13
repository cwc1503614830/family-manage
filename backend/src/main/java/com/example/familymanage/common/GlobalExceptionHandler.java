package com.example.familymanage.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception exception) {
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ApiResponse<Void> handleUnauthorized(UnauthorizedException exception) {
        return ApiResponse.fail(401, exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ApiResponse<Void> handleForbidden(ForbiddenException exception) {
        return ApiResponse.fail(403, exception.getMessage());
    }
}
