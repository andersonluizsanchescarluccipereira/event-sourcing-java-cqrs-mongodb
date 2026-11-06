package dev.lab.eventsourcing.application.port;

import dev.lab.eventsourcing.domain.AccountEvent;
import dev.lab.eventsourcing.domain.StoredEvent;
import java.util.List;
import java.util.UUID;

public interface AccountEventStore {
    List<StoredEvent> load(UUID accountId);
    void append(UUID accountId, long expectedRevision, AccountEvent event);
}
