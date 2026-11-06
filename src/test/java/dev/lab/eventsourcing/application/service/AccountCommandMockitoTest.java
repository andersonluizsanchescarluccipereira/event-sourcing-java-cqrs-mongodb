package dev.lab.eventsourcing.application.service;

import dev.lab.eventsourcing.application.port.AccountEventStore;
import dev.lab.eventsourcing.application.port.AccountProjectionPort;
import dev.lab.eventsourcing.domain.AccountEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountCommandMockitoTest {
    @Mock AccountEventStore store;
    @Mock AccountProjectionPort projection;
    @InjectMocks AccountCommandService commands;

    @Test void firstAppendRequiresNoStreamAndRegistersForProjection() {
        var id = UUID.randomUUID();
        when(store.load(id)).thenReturn(List.of());
        commands.open(id, "Ana");
        verify(projection).track(id);
        verify(store).append(eq(id), eq(-1L),
                argThat(event -> event instanceof AccountEvent.Opened opened && opened.owner().equals("Ana")));
        verifyNoMoreInteractions(store);
    }
}
