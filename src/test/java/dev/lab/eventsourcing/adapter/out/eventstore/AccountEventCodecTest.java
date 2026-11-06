package dev.lab.eventsourcing.adapter.out.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lab.eventsourcing.domain.AccountEvent;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AccountEventCodecTest {
    @Test void roundTripAllEventTypes() throws Exception {
        var mapper = new ObjectMapper();
        var codec = new AccountEventCodec(mapper);
        UUID id = UUID.randomUUID(), other = UUID.randomUUID(), transfer = UUID.randomUUID();
        var events = List.<AccountEvent>of(
                new AccountEvent.Opened(id, "Dora"),
                new AccountEvent.Deposited(id, new BigDecimal("10.00"), "PIX"),
                new AccountEvent.Withdrawn(id, new BigDecimal("5.00"), "ATM"),
                new AccountEvent.TransferDebited(id, transfer, other, new BigDecimal("2.00")),
                new AccountEvent.TransferCredited(id, transfer, other, new BigDecimal("2.00")));
        for (var event : events) {
            var eventId = UUID.randomUUID();
            var bytes = mapper.writeValueAsBytes(codec.encode(eventId, 1234, event));
            var decoded = codec.decode(codec.type(event), 3, bytes);
            assertEquals(event, decoded.payload());
            assertEquals(eventId, decoded.eventId());
            assertEquals(3, decoded.revision());
        }
    }
}
