package dev.lab.eventsourcing.support;

import dev.lab.eventsourcing.application.port.AccountEventStore;
import dev.lab.eventsourcing.application.port.AccountProjectionPort;
import dev.lab.eventsourcing.application.service.OptimisticConflictException;
import dev.lab.eventsourcing.domain.AccountEvent;
import dev.lab.eventsourcing.domain.StoredEvent;
import java.time.Instant;
import java.util.*;

public final class InMemoryAdapters {
    private InMemoryAdapters() {}

    public static class Store implements AccountEventStore {
        private final Map<UUID, List<StoredEvent>> streams = new HashMap<>();

        @Override public synchronized List<StoredEvent> load(UUID id) {
            return List.copyOf(streams.getOrDefault(id, List.of()));
        }

        @Override public synchronized void append(UUID id, long expected, AccountEvent event) {
            var stream = streams.computeIfAbsent(id, ignored -> new ArrayList<>());
            if (expected != stream.size() - 1)
                throw new OptimisticConflictException("Conflito de revisao");
            stream.add(new StoredEvent(UUID.randomUUID(), stream.size(), Instant.now(), event));
        }
    }

    public static class Projections implements AccountProjectionPort {
        private final Set<UUID> tracked = new HashSet<>();
        private final Map<UUID, View> views = new HashMap<>();

        @Override public void track(UUID id) { tracked.add(id); }
        @Override public List<UUID> trackedAccounts() { return List.copyOf(tracked); }
        @Override public Optional<View> find(UUID id) { return Optional.ofNullable(views.get(id)); }
        @Override public void save(View view) { views.put(view.accountId(), view); }
        @Override public void delete(UUID id) { views.remove(id); }
    }
}
