package dev.lab.eventsourcing.adapter.out.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AccountIndexRepository extends MongoRepository<AccountIndexDocument, String> {}
