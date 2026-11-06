package dev.lab.eventsourcing.domain;

public class DomainException extends RuntimeException {
    public DomainException(String message) { super(message); }
}
