package com.warehouse.demo.controller;

import com.warehouse.demo.dto.response.ErrorResponse;
import com.warehouse.demo.dto.response.ValidationFieldError;
import com.warehouse.demo.exception.BusinessLogicException;
import com.warehouse.demo.exception.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class ExceptionHandlerController {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(EntityNotFoundException ex,
                                                  HttpServletRequest request) {
        log.warn("Ошибка попытки получения сущности", ex);
        ErrorResponse body = new ErrorResponse(
                Instant.now(), 404, ex.getMessage(), request.getRequestURI(), List.of()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> notAccessible(BusinessLogicException ex,
                                                       HttpServletRequest request) {
        log.warn("Ошибка попытки редактирования предопределенной сущности");
        ErrorResponse body = new ErrorResponse(
                Instant.now(), 403, ex.getMessage(), request.getRequestURI(), List.of()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> notValid(MethodArgumentNotValidException ex,
                                                  HttpServletRequest request) {
        log.warn("Проверка заполнения полей сущности", ex);
        BindingResult bindingResult = ex.getBindingResult();
        List<ValidationFieldError> fieldErrors = bindingResult
                .getFieldErrors()
                .stream()
                .map(error -> new ValidationFieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        String message = fieldErrors
                .stream()
                .map(ValidationFieldError::message)
                .reduce((a, b) -> a + "; " + b)
                .orElse("Ошибка проверки данных");
        ErrorResponse body = new ErrorResponse(
                Instant.now(), 400, message, request.getRequestURI(), fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
