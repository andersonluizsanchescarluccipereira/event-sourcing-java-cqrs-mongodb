package dev.lab.eventsourcing.adapter.out.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tracked_accounts")
public class AccountIndexDocument {
    @Id
    public String id;

    public AccountIndexDocument() {}
    public AccountIndexDocument(String id) { this.id = id; }
}
