package dev.lab.eventsourcing.adapter.in.web;

import dev.lab.eventsourcing.application.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AccountController {
    private final AccountCommandService commands;
    private final AccountQueryService queries;
    private final ProjectionService projections;
    private final TransferService transfers;

    public AccountController(AccountCommandService commands, AccountQueryService queries,
                             ProjectionService projections, TransferService transfers) {
        this.commands = commands;
        this.queries = queries;
        this.projections = projections;
        this.transfers = transfers;
    }

    public record CreateAccountRequest(@NotNull UUID accountId, @NotBlank String owner) {}
    public record MoneyRequest(@NotNull @DecimalMin("0.01") BigDecimal amount, String reference) {}
    public record TransferRequest(@NotNull UUID transferId, @NotNull UUID from,
                                  @NotNull UUID to, @NotNull @DecimalMin("0.01") BigDecimal amount) {}

    @PostMapping("/accounts")
    public ResponseEntity<?> open(@Valid @RequestBody CreateAccountRequest request) {
        UUID id = commands.open(request.accountId(), request.owner());
        return ResponseEntity.created(URI.create("/api/accounts/" + id))
                .body(Map.of("accountId", id, "status", "EVENT_RECORDED"));
    }

    @PostMapping("/accounts/{id}/deposits")
    public ResponseEntity<?> deposit(@PathVariable UUID id, @Valid @RequestBody MoneyRequest request) {
        commands.deposit(id, request.amount(), request.reference());
        return ResponseEntity.accepted().body(Map.of("status", "EVENT_RECORDED"));
    }

    @PostMapping("/accounts/{id}/withdrawals")
    public ResponseEntity<?> withdraw(@PathVariable UUID id, @Valid @RequestBody MoneyRequest request) {
        commands.withdraw(id, request.amount(), request.reference());
        return ResponseEntity.accepted().body(Map.of("status", "EVENT_RECORDED"));
    }

    @PostMapping("/transfers")
    public ResponseEntity<?> transfer(@Valid @RequestBody TransferRequest request,
             @RequestHeader(name = "X-Lab-Fail-After-Debit", defaultValue = "false") boolean failAfterDebit) {
        transfers.transfer(request.transferId(), request.from(), request.to(),
                request.amount(), failAfterDebit);
        return ResponseEntity.accepted().body(Map.of("transferId", request.transferId(),
                "status", "COMPLETED_IN_EVENT_STORE", "projection", "EVENTUALLY_CONSISTENT"));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<?> projection(@PathVariable UUID id) {
        return queries.findProjection(id).<ResponseEntity<?>>map(view -> ResponseEntity.ok(view))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/accounts/{id}/state")
    public ResponseEntity<?> state(@PathVariable UUID id) {
        return ResponseEntity.ok(queries.strongState(id));
    }

    @GetMapping("/accounts/{id}/events")
    public ResponseEntity<?> events(@PathVariable UUID id) {
        return ResponseEntity.ok(queries.history(id));
    }

    @PostMapping("/accounts/{id}/replay")
    public ResponseEntity<?> replay(@PathVariable UUID id) {
        return ResponseEntity.ok(projections.replay(id));
    }
}
