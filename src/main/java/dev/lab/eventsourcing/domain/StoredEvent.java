package dev.lab.eventsourcing.domain;

import java.time.Instant;
import java.util.UUID;

/** Revision is 0-based within the stream; -1 means an empty stream. */
public record StoredEvent(UUID eventId, long revision, Instant occurredAt, AccountEvent payload) {}
