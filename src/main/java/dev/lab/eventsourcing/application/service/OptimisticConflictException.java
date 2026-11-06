package dev.lab.eventsourcing.application.service;

public class OptimisticConflictException extends RuntimeException {
    public OptimisticConflictException(String message) { super(message); }
}
