package dev.lab.eventsourcing.application.service;

import dev.lab.eventsourcing.domain.DomainException;
import dev.lab.eventsourcing.support.InMemoryAdapters;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AccountCommandServiceTest {
    @Test void appendsEventsWithExpectedRevision() {
        var store = new InMemoryAdapters.Store();
        var projections = new InMemoryAdapters.Projections();
        var service = new AccountCommandService(store, projections);
        var id = UUID.randomUUID();
        service.open(id, "Bruna");
        service.deposit(id, new BigDecimal("50.00"), "SALARY");
        service.withdraw(id, new BigDecimal("20.00"), "ATM");
        assertEquals(3, store.load(id).size());
        assertEquals(0, store.load(id).get(0).revision());
        assertEquals(2, store.load(id).get(2).revision());
        assertEquals(1, projections.trackedAccounts().size());
        assertThrows(DomainException.class, () -> service.open(id, "Bruna"));
    }
}
