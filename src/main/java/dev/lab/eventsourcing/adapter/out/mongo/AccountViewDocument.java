package dev.lab.eventsourcing.adapter.out.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;

@Document(collection = "account_projections")
public class AccountViewDocument {
    @Id
    public String id;
    public String owner;
    public BigDecimal balance;
    public long revision;

    public AccountViewDocument() {}
    public AccountViewDocument(String id, String owner, BigDecimal balance, long revision) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
        this.revision = revision;
    }
}
