package dev.lab.eventsourcing.domain;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountAggregateTest {
    private final UUID id = UUID.randomUUID();

    @Test void rehydratesHistoryWithoutLosingAuditTrail() {
        var history = List.of(
                stored(0, new AccountEvent.Opened(id, "Ana")),
                stored(1, new AccountEvent.Deposited(id, new BigDecimal("100.00"), "PIX")),
                stored(2, new AccountEvent.Withdrawn(id, new BigDecimal("20.50"), "ATM")));
        var aggregate = AccountAggregate.rehydrate(id, history);
        assertEquals(new BigDecimal("79.50"), aggregate.balance());
        assertEquals("Ana", aggregate.owner());
        assertEquals(2, aggregate.revision());
    }

    @Test void preventsOverdraftAndInvalidPrecision() {
        var aggregate = AccountAggregate.rehydrate(id, List.of(stored(0, new AccountEvent.Opened(id, "Ana"))));
        assertThrows(DomainException.class, () -> aggregate.withdraw(new BigDecimal("0.01"), ""));
        assertThrows(DomainException.class, () -> aggregate.deposit(new BigDecimal("1.001"), ""));
        assertThrows(DomainException.class, () -> aggregate.deposit(new BigDecimal("-2"), ""));
    }

    @Test void refusesBrokenStreamRevision() {
        var events = List.of(stored(1, new AccountEvent.Opened(id, "Ana")));
        assertThrows(IllegalStateException.class, () -> AccountAggregate.rehydrate(id, events));
    }

    private StoredEvent stored(long revision, AccountEvent event) {
        return new StoredEvent(UUID.randomUUID(), revision, Instant.now(), event);
    }
}
