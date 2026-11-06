package dev.lab.eventsourcing.application.service;

import dev.lab.eventsourcing.domain.AccountAggregate;
import dev.lab.eventsourcing.domain.DomainException;
import dev.lab.eventsourcing.support.InMemoryAdapters;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class TransferServiceTest {
    @Test void retriesPartialTransferWithoutDoubleDebitOrCredit() {
        var store = new InMemoryAdapters.Store();
        var commands = new AccountCommandService(store, new InMemoryAdapters.Projections());
        var service = new TransferService(store, true);
        var from = UUID.randomUUID();
        var to = UUID.randomUUID();
        var transferId = UUID.randomUUID();
        commands.open(from, "Origem");
        commands.open(to, "Destino");
        commands.deposit(from, new BigDecimal("100.00"), "FUNDING");
        assertThrows(LabInjectedFailureException.class,
                () -> service.transfer(transferId, from, to, new BigDecimal("30.00"), true));
        assertEquals(new BigDecimal("70.00"), state(store, from).balance());
        assertEquals(new BigDecimal("0.00"), state(store, to).balance());
        service.transfer(transferId, from, to, new BigDecimal("30.00"), false);
        service.transfer(transferId, from, to, new BigDecimal("30.00"), false);
        assertEquals(new BigDecimal("70.00"), state(store, from).balance());
        assertEquals(new BigDecimal("30.00"), state(store, to).balance());
        assertEquals(3, store.load(from).size());
        assertEquals(2, store.load(to).size());
        assertThrows(DomainException.class,
                () -> service.transfer(transferId, from, to, new BigDecimal("40.00"), false));
    }

    @Test void rejectsUnfundedTransferBeforeDebit() {
        var store = new InMemoryAdapters.Store();
        var commands = new AccountCommandService(store, new InMemoryAdapters.Projections());
        var from = UUID.randomUUID();
        var to = UUID.randomUUID();
        commands.open(from, "Origem");
        commands.open(to, "Destino");
        var service = new TransferService(store, true);
        assertThrows(DomainException.class,
                () -> service.transfer(UUID.randomUUID(), from, to, new BigDecimal("1.00"), false));
        assertEquals(1, store.load(from).size());
    }

    private AccountAggregate state(InMemoryAdapters.Store store, UUID id) {
        return AccountAggregate.rehydrate(id, store.load(id));
    }
}
