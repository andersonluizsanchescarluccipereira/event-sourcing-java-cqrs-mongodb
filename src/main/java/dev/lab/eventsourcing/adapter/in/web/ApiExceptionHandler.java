package dev.lab.eventsourcing.adapter.in.web;

import dev.lab.eventsourcing.application.service.LabInjectedFailureException;
import dev.lab.eventsourcing.application.service.OptimisticConflictException;
import dev.lab.eventsourcing.domain.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(OptimisticConflictException.class)
    public ResponseEntity<?> conflict(OptimisticConflictException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(LabInjectedFailureException.class)
    public ResponseEntity<?> injected(LabInjectedFailureException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<?> domain(DomainException e) {
        var status = e.getMessage().equals("Conta nao encontrada") ? HttpStatus.NOT_FOUND
                : e.getMessage().equals("Conta ja existe") ? HttpStatus.CONFLICT
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return error(status, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        return error(HttpStatus.BAD_REQUEST, "JSON invalido ou campos obrigatorios ausentes");
    }

    private ResponseEntity<?> error(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "error", detail));
    }
}
