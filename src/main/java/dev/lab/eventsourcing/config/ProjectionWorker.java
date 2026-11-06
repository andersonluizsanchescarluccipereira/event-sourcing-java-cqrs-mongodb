package dev.lab.eventsourcing.config;

import dev.lab.eventsourcing.application.port.AccountProjectionPort;
import dev.lab.eventsourcing.application.service.ProjectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProjectionWorker {
    private static final Logger log = LoggerFactory.getLogger(ProjectionWorker.class);
    private final AccountProjectionPort projections;
    private final ProjectionService service;

    public ProjectionWorker(AccountProjectionPort projections, ProjectionService service) {
        this.projections = projections;
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.projection.interval-ms:2000}")
    public void poll() {
        for (var id : projections.trackedAccounts()) {
            try {
                service.synchronize(id);
            } catch (Exception ex) {
                log.error("Falha ao projetar conta {}. Proximo ciclo tentara novamente.", id, ex);
            }
        }
    }
}
