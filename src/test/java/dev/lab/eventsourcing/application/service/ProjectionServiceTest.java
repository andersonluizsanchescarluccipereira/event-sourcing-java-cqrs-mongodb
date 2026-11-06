package dev.lab.eventsourcing.application.service;

import dev.lab.eventsourcing.support.InMemoryAdapters;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProjectionServiceTest {
    @Test void synchronizesAndRebuildsReadModelFromImmutableEvents() {
        var store = new InMemoryAdapters.Store();
        var projections = new InMemoryAdapters.Projections();
        var commands = new AccountCommandService(store, projections);
        var service = new ProjectionService(store, projections);
        var id = UUID.randomUUID();
        commands.open(id, "Clara");
        commands.deposit(id, new BigDecimal("123.45"), "PIX");
        assertTrue(projections.find(id).isEmpty()); // eventual consistency, before worker
        service.synchronize(id);
        assertEquals(new BigDecimal("123.45"), projections.find(id).orElseThrow().balance());
        assertEquals(1, projections.find(id).orElseThrow().revision());
        projections.delete(id); // simulate loss of MongoDB read model
        assertTrue(projections.find(id).isEmpty());
        var restored = service.replay(id);
        assertEquals(new BigDecimal("123.45"), restored.balance());
        assertEquals(2, store.load(id).size()); // source of truth was not touched
    }
}
