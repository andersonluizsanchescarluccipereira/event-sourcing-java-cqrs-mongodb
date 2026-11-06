package dev.lab.eventsourcing.config;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.EventStoreDBClientSettings;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import dev.lab.eventsourcing.application.port.AccountEventStore;
import dev.lab.eventsourcing.application.port.AccountProjectionPort;
import dev.lab.eventsourcing.application.service.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfiguration {
    @Bean
    EventStoreDBClient eventStoreDBClient(@Value("${app.eventstore.uri}") String uri) throws Exception {
        EventStoreDBClientSettings settings = EventStoreDBConnectionString.parseOrThrow(uri);
        return EventStoreDBClient.create(settings);
    }

    @Bean
    AccountCommandService accountCommandService(AccountEventStore store, AccountProjectionPort projection) {
        return new AccountCommandService(store, projection);
    }

    @Bean
    AccountQueryService accountQueryService(AccountEventStore store, AccountProjectionPort projection) {
        return new AccountQueryService(store, projection);
    }

    @Bean
    ProjectionService projectionService(AccountEventStore store, AccountProjectionPort projection) {
        return new ProjectionService(store, projection);
    }

    @Bean
    TransferService transferService(AccountEventStore store,
                                    @Value("${app.lab.enabled:false}") boolean labEnabled) {
        return new TransferService(store, labEnabled);
    }
}
