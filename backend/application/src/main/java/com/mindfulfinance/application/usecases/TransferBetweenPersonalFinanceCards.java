package com.mindfulfinance.application.usecases;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Objects;

import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.application.ports.TransactionRepository;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;
import com.mindfulfinance.domain.transaction.Transaction;
import com.mindfulfinance.domain.transaction.TransactionDirection;
import com.mindfulfinance.domain.transaction.TransactionId;

public final class TransferBetweenPersonalFinanceCards {
    static final String SAME_CARD_TRANSFER_MESSAGE = "Transfer source and destination cards must be different";
    static final String POSITIVE_AMOUNT_REQUIRED_MESSAGE = "Transfer amount must be positive RUB";
    static final String TRANSFER_DATE_REQUIRED_MESSAGE = "Transfer date must be provided";

    private static final Currency RUB = Currency.getInstance("RUB");

    private final PersonalFinanceCardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final Clock clock;

    public TransferBetweenPersonalFinanceCards(
        PersonalFinanceCardRepository cardRepository,
        TransactionRepository transactionRepository
    ) {
        this(cardRepository, transactionRepository, Clock.systemUTC());
    }

    TransferBetweenPersonalFinanceCards(
        PersonalFinanceCardRepository cardRepository,
        TransactionRepository transactionRepository,
        Clock clock
    ) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.clock = clock;
    }

    public void transfer(Command command) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(command.sourceCardId(), "sourceCardId");
        Objects.requireNonNull(command.destinationCardId(), "destinationCardId");
        if (command.occurredOn() == null) {
            throw new IllegalArgumentException(TRANSFER_DATE_REQUIRED_MESSAGE);
        }

        if (command.sourceCardId().equals(command.destinationCardId())) {
            throw new IllegalArgumentException(SAME_CARD_TRANSFER_MESSAGE);
        }

        validateAmount(command.amount());

        PersonalFinanceCard sourceCard = PersonalFinanceCardStateGuard.requireMutableCard(cardRepository, command.sourceCardId());
        PersonalFinanceCard destinationCard = PersonalFinanceCardStateGuard.requireMutableCard(cardRepository, command.destinationCardId());
        Instant createdAt = Instant.now(clock);
        Money amount = new Money(command.amount(), RUB);
        String memo = transferMemo(sourceCard.id(), destinationCard.id());

        transactionRepository.save(new Transaction(
            TransactionId.random(),
            sourceCard.linkedAccountId(),
            command.occurredOn(),
            TransactionDirection.OUTFLOW,
            amount,
            memo,
            createdAt
        ));
        transactionRepository.save(new Transaction(
            TransactionId.random(),
            destinationCard.linkedAccountId(),
            command.occurredOn(),
            TransactionDirection.INFLOW,
            amount,
            memo,
            createdAt
        ));
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(POSITIVE_AMOUNT_REQUIRED_MESSAGE);
        }
    }

    private static String transferMemo(PersonalFinanceCardId sourceCardId, PersonalFinanceCardId destinationCardId) {
        return "[personal-finance:card-transfer:%s->%s]".formatted(
            sourceCardId.value(),
            destinationCardId.value()
        );
    }

    public record Command(
        PersonalFinanceCardId sourceCardId,
        PersonalFinanceCardId destinationCardId,
        LocalDate occurredOn,
        BigDecimal amount
    ) {}
}
