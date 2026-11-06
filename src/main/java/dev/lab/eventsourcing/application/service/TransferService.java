package dev.lab.eventsourcing.application.service;

import dev.lab.eventsourcing.application.port.AccountEventStore;
import dev.lab.eventsourcing.domain.AccountAggregate;
import dev.lab.eventsourcing.domain.DomainException;
import java.math.BigDecimal;
import java.util.UUID;

/** Educational two-stream saga: a failed credit is resumed with the same transferId. */
public final class TransferService {
    private final AccountEventStore store;
    private final boolean labEnabled;

    public TransferService(AccountEventStore store, boolean labEnabled) {
        this.store = store;
        this.labEnabled = labEnabled;
    }

    public void transfer(UUID transferId, UUID from, UUID to, BigDecimal amount,
                         boolean failAfterDebit) {
        if (transferId == null || from == null || to == null || from.equals(to))
            throw new DomainException("IDs de transferencia/contas invalidos");
        if (failAfterDebit && !labEnabled) throw new DomainException("Modo laboratorio desativado");
        BigDecimal normalized = AccountAggregate.positiveMoney(amount);
        var source = AccountAggregate.rehydrate(from, store.load(from));
        var destination = AccountAggregate.rehydrate(to, store.load(to));
        if (!source.opened() || !destination.opened())
            throw new DomainException("Origem e destino devem existir");

        var debit = source.debitTransfer(transferId, to, normalized);
        if (debit != null) store.append(from, source.revision(), debit);

        if (failAfterDebit && labEnabled) throw new LabInjectedFailureException();

        var credit = destination.creditTransfer(transferId, from, normalized);
        if (credit != null) store.append(to, destination.revision(), credit);
    }
}
