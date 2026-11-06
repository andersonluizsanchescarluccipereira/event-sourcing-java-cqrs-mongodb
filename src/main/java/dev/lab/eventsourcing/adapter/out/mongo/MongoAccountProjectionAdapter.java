package dev.lab.eventsourcing.adapter.out.mongo;

import dev.lab.eventsourcing.application.port.AccountProjectionPort;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MongoAccountProjectionAdapter implements AccountProjectionPort {
    private final AccountIndexRepository indexes;
    private final AccountViewRepository views;

    public MongoAccountProjectionAdapter(AccountIndexRepository indexes, AccountViewRepository views) {
        this.indexes = indexes;
        this.views = views;
    }

    @Override public void track(UUID id) { indexes.save(new AccountIndexDocument(id.toString())); }

    @Override public List<UUID> trackedAccounts() {
        return indexes.findAll().stream().map(doc -> UUID.fromString(doc.id)).toList();
    }

    @Override public Optional<View> find(UUID id) {
        return views.findById(id.toString()).map(doc ->
                new View(id, doc.owner, doc.balance, doc.revision));
    }

    @Override public void save(View view) {
        views.save(new AccountViewDocument(view.accountId().toString(), view.owner(),
                view.balance(), view.revision()));
    }

    @Override public void delete(UUID id) { views.deleteById(id.toString()); }
}
