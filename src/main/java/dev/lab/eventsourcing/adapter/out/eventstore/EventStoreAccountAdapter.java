package dev.lab.eventsourcing.adapter.out.eventstore;

import com.eventstore.dbclient.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lab.eventsourcing.application.port.AccountEventStore;
import dev.lab.eventsourcing.application.service.OptimisticConflictException;
import dev.lab.eventsourcing.domain.AccountEvent;
import dev.lab.eventsourcing.domain.StoredEvent;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Repository
public class EventStoreAccountAdapter implements AccountEventStore {
    private static final int PAGE_SIZE = 500;
    private final EventStoreDBClient client;
    private final AccountEventCodec codec;

    public EventStoreAccountAdapter(EventStoreDBClient client, ObjectMapper mapper) {
        this.client = client;
        this.codec = new AccountEventCodec(mapper);
    }

    @Override
    public List<StoredEvent> load(UUID accountId) {
        var events = new ArrayList<StoredEvent>();
        long offset = 0;
        while (true) {
            var options = ReadStreamOptions.get().forwards().fromRevision(offset).maxCount(PAGE_SIZE);
            List<ResolvedEvent> page;
            try {
                page = client.readStream(stream(accountId), options).get().getEvents();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Leitura interrompida", ex);
            } catch (ExecutionException ex) {
                if (ex.getCause() instanceof StreamNotFoundException) return List.of();
                throw new IllegalStateException("Falha na leitura do EventStoreDB", ex.getCause());
            }
            for (var resolved : page) {
                var recorded = resolved.getOriginalEvent();
                events.add(codec.decode(recorded.getEventType(), offset++, recorded.getEventData()));
            }
            if (page.size() < PAGE_SIZE) break;
        }
        return List.copyOf(events);
    }

    @Override
    public void append(UUID accountId, long expectedRevision, AccountEvent event) {
        UUID eventId = UUID.randomUUID();
        var data = EventData.builderAsJson(eventId, codec.type(event),
                codec.encode(eventId, System.currentTimeMillis(), event)).build();
        var revision = expectedRevision == -1
                ? ExpectedRevision.noStream()
                : ExpectedRevision.expectedRevision(expectedRevision);
        var options = AppendToStreamOptions.get().expectedRevision(revision);
        try {
            client.appendToStream(stream(accountId), options, data).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Escrita interrompida", ex);
        } catch (ExecutionException ex) {
            if (ex.getCause() instanceof WrongExpectedVersionException) {
                throw new OptimisticConflictException("Conflito de revisao. Releia o stream e tente novamente.");
            }
            throw new IllegalStateException("Falha na escrita do EventStoreDB", ex.getCause());
        }
    }

    private static String stream(UUID id) { return "account-" + id; }
}
