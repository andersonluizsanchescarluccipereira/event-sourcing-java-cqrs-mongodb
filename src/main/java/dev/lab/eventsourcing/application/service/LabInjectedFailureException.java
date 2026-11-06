package dev.lab.eventsourcing.application.service;

public class LabInjectedFailureException extends RuntimeException {
    public LabInjectedFailureException() {
        super("Falha simulada apos debito. Reenvie o MESMO transferId para concluir o credito.");
    }
}
