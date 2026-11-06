package dev.lab.eventsourcing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.lab.eventsourcing.domain.AccountEvent.*;

/** Pure domain model: no Spring, MongoDB or EventStoreDB dependency. */
public final class AccountAggregate {
    private final UUID accountId;
    private String owner;
    private BigDecimal balance = new BigDecimal("0.00");
    private long revision = -1;
    private boolean opened;
    private final Map<UUID, TransferDebited> debits = new HashMap<>();
    private final Map<UUID, TransferCredited> credits = new HashMap<>();

    private AccountAggregate(UUID accountId) { this.accountId = accountId; }

    public static AccountAggregate rehydrate(UUID accountId, List<StoredEvent> history) {
        var account = new AccountAggregate(accountId);
        for (var stored : history) {
            if (!accountId.equals(stored.payload().accountId())) {
                throw new IllegalStateException("Evento de outra conta no stream: " + accountId);
            }
            if (stored.revision() != account.revision + 1) {
                throw new IllegalStateException("Revisao descontinua no stream: " + accountId);
            }
            account.apply(stored.payload());
            account.revision = stored.revision();
        }
        return account;
    }

    private void apply(AccountEvent event) {
        switch (event) {
            case Opened e -> {
                if (opened) throw new IllegalStateException("Conta aberta duas vezes");
                opened = true;
                owner = e.owner();
            }
            case Deposited e -> { ensureOpened(); balance = balance.add(e.amount()); }
            case Withdrawn e -> { ensureOpened(); balance = balance.subtract(e.amount()); }
            case TransferDebited e -> {
                ensureOpened();
                balance = balance.subtract(e.amount());
                debits.put(e.transferId(), e);
            }
            case TransferCredited e -> {
                ensureOpened();
                balance = balance.add(e.amount());
                credits.put(e.transferId(), e);
            }
        }
    }

    public Opened open(String owner) {
        if (opened) throw new DomainException("Conta ja existe");
        if (owner == null || owner.isBlank()) throw new DomainException("Titular obrigatorio");
        return new Opened(accountId, owner.trim());
    }

    public Deposited deposit(BigDecimal amount, String reference) {
        ensureOpened();
        return new Deposited(accountId, positiveMoney(amount), normalizeReference(reference));
    }

    public Withdrawn withdraw(BigDecimal amount, String reference) {
        ensureOpened();
        BigDecimal normalized = positiveMoney(amount);
        checkBalance(normalized);
        return new Withdrawn(accountId, normalized, normalizeReference(reference));
    }

    public TransferDebited debitTransfer(UUID transferId, UUID destination, BigDecimal amount) {
        ensureOpened();
        if (transferId == null || destination == null || destination.equals(accountId))
            throw new DomainException("Transferencia invalida");
        var normalized = positiveMoney(amount);
        var previous = debits.get(transferId);
        if (previous != null) {
            if (!previous.destinationAccountId().equals(destination) || previous.amount().compareTo(normalized) != 0)
                throw new DomainException("TransferId reutilizado com dados diferentes");
            return null; // already debited: safe retry
        }
        checkBalance(normalized);
        return new TransferDebited(accountId, transferId, destination, normalized);
    }

    public TransferCredited creditTransfer(UUID transferId, UUID source, BigDecimal amount) {
        ensureOpened();
        if (transferId == null || source == null || source.equals(accountId))
            throw new DomainException("Transferencia invalida");
        var normalized = positiveMoney(amount);
        var previous = credits.get(transferId);
        if (previous != null) {
            if (!previous.sourceAccountId().equals(source) || previous.amount().compareTo(normalized) != 0)
                throw new DomainException("TransferId reutilizado com dados diferentes");
            return null;
        }
        return new TransferCredited(accountId, transferId, source, normalized);
    }

    private void checkBalance(BigDecimal amount) {
        if (balance.compareTo(amount) < 0) throw new DomainException("Saldo insuficiente");
    }

    private void ensureOpened() {
        if (!opened) throw new DomainException("Conta nao encontrada");
    }

    public static BigDecimal positiveMoney(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) throw new DomainException("Valor deve ser positivo");
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new DomainException("Valor aceita no maximo 2 casas decimais");
        }
    }

    private static String normalizeReference(String reference) {
        return reference == null ? "" : reference.trim();
    }

    public UUID accountId() { return accountId; }
    public String owner() { return owner; }
    public BigDecimal balance() { return balance; }
    public long revision() { return revision; }
    public boolean opened() { return opened; }
}
