package dev.lab.eventsourcing.application.service;

import dev.lab.eventsourcing.application.port.AccountEventStore;
import dev.lab.eventsourcing.application.port.AccountProjectionPort;
import dev.lab.eventsourcing.domain.AccountAggregate;
import dev.lab.eventsourcing.domain.DomainException;
import java.math.BigDecimal;
import java.util.UUID;

public final class AccountCommandService {
    private final AccountEventStore store;
    private final AccountProjectionPort projections;

    public AccountCommandService(AccountEventStore store, AccountProjectionPort projections) {
        this.store = store;
        this.projections = projections;
    }

    public UUID open(UUID id, String owner) {
        if (id == null) throw new DomainException("ID obrigatorio");
        var aggregate = load(id);
        var event = aggregate.open(owner);
        // Register before writing, so a successful append is always discoverable by the poller.
        // A failed append may leave an inert tracking record; it has no monetary effect.
        projections.track(id);
        store.append(id, aggregate.revision(), event);
        return id;
    }

    public void deposit(UUID id, BigDecimal amount, String reference) {
        var aggregate = load(id);
        store.append(id, aggregate.revision(), aggregate.deposit(amount, reference));
    }

    public void withdraw(UUID id, BigDecimal amount, String reference) {
        var aggregate = load(id);
        store.append(id, aggregate.revision(), aggregate.withdraw(amount, reference));
    }

    private AccountAggregate load(UUID id) {
        return AccountAggregate.rehydrate(id, store.load(id));
    }
}
