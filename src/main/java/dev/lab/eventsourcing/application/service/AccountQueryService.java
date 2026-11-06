package dev.lab.eventsourcing.application.service;

import dev.lab.eventsourcing.application.port.AccountEventStore;
import dev.lab.eventsourcing.application.port.AccountProjectionPort;
import dev.lab.eventsourcing.domain.AccountAggregate;
import dev.lab.eventsourcing.domain.DomainException;
import dev.lab.eventsourcing.domain.StoredEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class AccountQueryService {
    private final AccountEventStore store;
    private final AccountProjectionPort projections;

    public AccountQueryService(AccountEventStore store, AccountProjectionPort projections) {
        this.store = store;
        this.projections = projections;
    }

    public Optional<AccountProjectionPort.View> findProjection(UUID id) { return projections.find(id); }
    public List<StoredEvent> history(UUID id) { return store.load(id); }
    public AccountProjectionPort.View strongState(UUID id) {
        var aggregate = AccountAggregate.rehydrate(id, store.load(id));
        if (!aggregate.opened()) throw new DomainException("Conta nao encontrada");
        return new AccountProjectionPort.View(id, aggregate.owner(), aggregate.balance(), aggregate.revision());
    }
}
