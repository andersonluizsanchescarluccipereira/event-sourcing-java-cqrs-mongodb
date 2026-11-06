package dev.lab.eventsourcing.application.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountProjectionPort {
    record View(UUID accountId, String owner, BigDecimal balance, long revision) {}
    void track(UUID accountId);
    List<UUID> trackedAccounts();
    Optional<View> find(UUID accountId);
    void save(View view);
    void delete(UUID accountId);
}
