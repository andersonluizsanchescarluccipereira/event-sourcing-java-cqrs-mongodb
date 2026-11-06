package dev.lab.eventsourcing.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Events are facts: never edit or delete a previously persisted event. */
public sealed interface AccountEvent permits AccountEvent.Opened, AccountEvent.Deposited,
        AccountEvent.Withdrawn, AccountEvent.TransferDebited, AccountEvent.TransferCredited {
    UUID accountId();

    record Opened(UUID accountId, String owner) implements AccountEvent {}
    record Deposited(UUID accountId, BigDecimal amount, String reference) implements AccountEvent {}
    record Withdrawn(UUID accountId, BigDecimal amount, String reference) implements AccountEvent {}
    record TransferDebited(UUID accountId, UUID transferId, UUID destinationAccountId,
                           BigDecimal amount) implements AccountEvent {}
    record TransferCredited(UUID accountId, UUID transferId, UUID sourceAccountId,
                            BigDecimal amount) implements AccountEvent {}
}
