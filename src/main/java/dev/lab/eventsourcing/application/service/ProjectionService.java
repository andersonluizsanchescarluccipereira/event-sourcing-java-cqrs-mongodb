package dev.lab.eventsourcing.application.service;

import dev.lab.eventsourcing.application.port.AccountEventStore;
import dev.lab.eventsourcing.application.port.AccountProjectionPort;
import dev.lab.eventsourcing.domain.AccountAggregate;
import dev.lab.eventsourcing.domain.DomainException;
import java.util.UUID;

/** Deterministic, idempotent full-stream rebuild; simple poller for a local lab. */
public final class ProjectionService {
    private final AccountEventStore store;
    private final AccountProjectionPort projections;

    public ProjectionService(AccountEventStore store, AccountProjectionPort projections) {
        this.store = store;
        this.projections = projections;
    }

    public void synchronize(UUID id) {
        var aggregate = AccountAggregate.rehydrate(id, store.load(id));
        if (!aggregate.opened()) return;
        var current = projections.find(id);
        if (current.isPresent() && current.get().revision() == aggregate.revision()) return;
        projections.save(new AccountProjectionPort.View(id, aggregate.owner(),
                aggregate.balance(), aggregate.revision()));
    }

    public AccountProjectionPort.View replay(UUID id) {
        var aggregate = AccountAggregate.rehydrate(id, store.load(id));
        if (!aggregate.opened()) throw new DomainException("Conta nao encontrada");
        projections.delete(id);
        synchronize(id);
        return projections.find(id).orElseThrow();
    }
}
