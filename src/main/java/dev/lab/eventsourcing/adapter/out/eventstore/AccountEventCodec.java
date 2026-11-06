package dev.lab.eventsourcing.adapter.out.eventstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lab.eventsourcing.domain.AccountEvent;
import dev.lab.eventsourcing.domain.StoredEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static dev.lab.eventsourcing.domain.AccountEvent.*;

/** Explicit, versioned wire schema, independent of Java class names. */
public final class AccountEventCodec {
    private final ObjectMapper mapper;

    public AccountEventCodec(ObjectMapper mapper) { this.mapper = mapper; }

    public String type(AccountEvent event) {
        return switch (event) {
            case Opened ignored -> "account.opened.v1";
            case Deposited ignored -> "account.deposited.v1";
            case Withdrawn ignored -> "account.withdrawn.v1";
            case TransferDebited ignored -> "account.transfer-debited.v1";
            case TransferCredited ignored -> "account.transfer-credited.v1";
        };
    }

    public Map<String, Object> encode(UUID eventId, long timestampMs, AccountEvent event) {
        var data = new HashMap<String, Object>();
        data.put("eventId", eventId.toString());
        data.put("occurredAtEpochMs", timestampMs);
        data.put("accountId", event.accountId().toString());
        switch (event) {
            case Opened e -> data.put("owner", e.owner());
            case Deposited e -> { data.put("amount", e.amount().toPlainString()); data.put("reference", e.reference()); }
            case Withdrawn e -> { data.put("amount", e.amount().toPlainString()); data.put("reference", e.reference()); }
            case TransferDebited e -> {
                data.put("transferId", e.transferId().toString());
                data.put("destinationAccountId", e.destinationAccountId().toString());
                data.put("amount", e.amount().toPlainString());
            }
            case TransferCredited e -> {
                data.put("transferId", e.transferId().toString());
                data.put("sourceAccountId", e.sourceAccountId().toString());
                data.put("amount", e.amount().toPlainString());
            }
        }
        return data;
    }

    public StoredEvent decode(String type, long revision, byte[] json) {
        try {
            JsonNode node = mapper.readTree(json);
            UUID id = UUID.fromString(required(node, "accountId"));
            AccountEvent payload = switch (type) {
                case "account.opened.v1" -> new Opened(id, required(node, "owner"));
                case "account.deposited.v1" -> new Deposited(id, money(node), required(node, "reference"));
                case "account.withdrawn.v1" -> new Withdrawn(id, money(node), required(node, "reference"));
                case "account.transfer-debited.v1" -> new TransferDebited(id,
                        UUID.fromString(required(node, "transferId")),
                        UUID.fromString(required(node, "destinationAccountId")), money(node));
                case "account.transfer-credited.v1" -> new TransferCredited(id,
                        UUID.fromString(required(node, "transferId")),
                        UUID.fromString(required(node, "sourceAccountId")), money(node));
                default -> throw new IllegalStateException("Tipo de evento desconhecido: " + type);
            };
            return new StoredEvent(UUID.fromString(required(node, "eventId")), revision,
                    Instant.ofEpochMilli(node.path("occurredAtEpochMs").asLong()), payload);
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Payload de evento invalido: " + type, ex);
        }
    }

    private static String required(JsonNode node, String name) {
        var value = node.path(name);
        if (value.isMissingNode() || value.isNull()) throw new IllegalStateException("Campo ausente: " + name);
        return value.asText();
    }

    private static BigDecimal money(JsonNode node) {
        return new BigDecimal(required(node, "amount"));
    }
}
