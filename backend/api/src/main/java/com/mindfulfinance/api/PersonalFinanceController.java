package com.mindfulfinance.api;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.mindfulfinance.application.ports.PersonalFinanceCardRepository;
import com.mindfulfinance.application.usecases.ArchivePersonalFinanceCard;
import com.mindfulfinance.application.usecases.DeletePersonalFinanceCard;
import com.mindfulfinance.application.usecases.CreatePersonalFinanceCard;
import com.mindfulfinance.application.usecases.GetCardPersonalFinanceSnapshot;
import com.mindfulfinance.application.usecases.ListPersonalFinanceCards;
import com.mindfulfinance.application.usecases.RenamePersonalFinanceCard;
import com.mindfulfinance.application.usecases.RestorePersonalFinanceCard;
import com.mindfulfinance.application.usecases.SaveMonthlyExpenseActual;
import com.mindfulfinance.application.usecases.SaveMonthlyIncomeActual;
import com.mindfulfinance.application.usecases.SavePersonalFinanceSettings;
import com.mindfulfinance.domain.money.Money;
import com.mindfulfinance.domain.personalfinance.IncomeForecast;
import com.mindfulfinance.domain.personalfinance.PersonalExpenseCategory;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCard;
import com.mindfulfinance.domain.personalfinance.PersonalFinanceCardId;

@RestController
public class PersonalFinanceController {
    private final PersonalFinanceCardRepository cardRepository;
    private final ListPersonalFinanceCards listPersonalFinanceCards;
    private final CreatePersonalFinanceCard createPersonalFinanceCard;
    private final RenamePersonalFinanceCard renamePersonalFinanceCard;
    private final ArchivePersonalFinanceCard archivePersonalFinanceCard;
    private final RestorePersonalFinanceCard restorePersonalFinanceCard;
    private final DeletePersonalFinanceCard deletePersonalFinanceCard;
    private final GetCardPersonalFinanceSnapshot getCardPersonalFinanceSnapshot;
    private final SaveMonthlyExpenseActual saveMonthlyExpenseActual;
    private final SaveMonthlyIncomeActual saveMonthlyIncomeActual;
    private final SavePersonalFinanceSettings savePersonalFinanceSettings;

    public PersonalFinanceController(
        PersonalFinanceCardRepository cardRepository,
        ListPersonalFinanceCards listPersonalFinanceCards,
        CreatePersonalFinanceCard createPersonalFinanceCard,
        RenamePersonalFinanceCard renamePersonalFinanceCard,
        ArchivePersonalFinanceCard archivePersonalFinanceCard,
        RestorePersonalFinanceCard restorePersonalFinanceCard,
        DeletePersonalFinanceCard deletePersonalFinanceCard,
        GetCardPersonalFinanceSnapshot getCardPersonalFinanceSnapshot,
        SaveMonthlyExpenseActual saveMonthlyExpenseActual,
        SaveMonthlyIncomeActual saveMonthlyIncomeActual,
        SavePersonalFinanceSettings savePersonalFinanceSettings
    ) {
        this.cardRepository = cardRepository;
        this.listPersonalFinanceCards = listPersonalFinanceCards;
        this.createPersonalFinanceCard = createPersonalFinanceCard;
        this.renamePersonalFinanceCard = renamePersonalFinanceCard;
        this.archivePersonalFinanceCard = archivePersonalFinanceCard;
        this.restorePersonalFinanceCard = restorePersonalFinanceCard;
        this.deletePersonalFinanceCard = deletePersonalFinanceCard;
        this.getCardPersonalFinanceSnapshot = getCardPersonalFinanceSnapshot;
        this.saveMonthlyExpenseActual = saveMonthlyExpenseActual;
        this.saveMonthlyIncomeActual = saveMonthlyIncomeActual;
        this.savePersonalFinanceSettings = savePersonalFinanceSettings;
    }

    @GetMapping("/personal-finance/cards")
    public List<PersonalFinanceCardDto> listCards() {
        return listPersonalFinanceCards.list().stream()
            .map(PersonalFinanceController::toCardDto)
            .toList();
    }

    @PostMapping("/personal-finance/cards")
    public CreatePersonalFinanceCardResponse createCard(@RequestBody CreatePersonalFinanceCardRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }

        PersonalFinanceCard card = createPersonalFinanceCard.create(new CreatePersonalFinanceCard.Command(request.name()));
        return new CreatePersonalFinanceCardResponse(card.id().value().toString());
    }

    @PutMapping("/personal-finance/cards/{cardId}")
    @Transactional
    public ResponseEntity<Void> renameCard(
        @PathVariable("cardId") String rawCardId,
        @RequestBody UpdatePersonalFinanceCardRequest request
    ) {
        PersonalFinanceCardId cardId = requireExistingCardId(rawCardId);
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }

        renamePersonalFinanceCard.rename(new RenamePersonalFinanceCard.Command(cardId, request.name()));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/personal-finance/cards/{cardId}/archive")
    @Transactional
    public ResponseEntity<Void> archiveCard(@PathVariable("cardId") String rawCardId) {
        PersonalFinanceCardId cardId = requireExistingCardId(rawCardId);
        archivePersonalFinanceCard.archive(new ArchivePersonalFinanceCard.Command(cardId));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/personal-finance/cards/{cardId}/restore")
    @Transactional
    public ResponseEntity<Void> restoreCard(@PathVariable("cardId") String rawCardId) {
        PersonalFinanceCardId cardId = requireExistingCardId(rawCardId);
        restorePersonalFinanceCard.restore(new RestorePersonalFinanceCard.Command(cardId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/personal-finance/cards/{cardId}")
    @Transactional
    public ResponseEntity<Void> deleteCard(@PathVariable("cardId") String rawCardId) {
        PersonalFinanceCardId cardId = requireExistingCardId(rawCardId);
        deletePersonalFinanceCard.delete(new DeletePersonalFinanceCard.Command(cardId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/personal-finance/cards/{cardId}/years/{year}")
    public PersonalFinanceSnapshotDto getSnapshot(
        @PathVariable("cardId") String rawCardId,
        @PathVariable("year") int year
    ) {
        validateYear(year);
        PersonalFinanceCardId cardId = requireExistingCardId(rawCardId);
        return toDto(getCardPersonalFinanceSnapshot.get(cardId, year));
    }

    @PutMapping("/personal-finance/cards/{cardId}/expenses/actual/{month}")
    public ResponseEntity<Void> updateMonthlyExpenseActual(
        @PathVariable("cardId") String rawCardId,
        @PathVariable("month") int month,
        @RequestBody UpdateMonthlyExpenseRequest request
    ) {
        PersonalFinanceCardId cardId = requireExistingCardId(rawCardId);
        validateMonth(month);

        saveMonthlyExpenseActual.save(new SaveMonthlyExpenseActual.Command(
            cardId,
            validateYear(request.year()),
            month,
            toExpenseCategoryAmounts(request.categoryAmounts(), "Category amounts must not be null")
        ));

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/personal-finance/cards/{cardId}/income/actual/{month}")
    public ResponseEntity<Void> updateMonthlyIncomeActual(
        @PathVariable("cardId") String rawCardId,
        @PathVariable("month") int month,
        @RequestBody UpdateMonthlyIncomeActualRequest request
    ) {
        PersonalFinanceCardId cardId = requireExistingCardId(rawCardId);
        validateMonth(month);
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }

        saveMonthlyIncomeActual.save(new SaveMonthlyIncomeActual.Command(
            cardId,
            validateYear(request.year()),
            month,
            request.totalAmount()
        ));

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/personal-finance/cards/{cardId}/settings")
    public ResponseEntity<Void> updateSettings(
        @PathVariable("cardId") String rawCardId,
        @RequestBody UpdateCardSettingsRequest request
    ) {
        PersonalFinanceCardId cardId = requireExistingCardId(rawCardId);
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }

        savePersonalFinanceSettings.save(new SavePersonalFinanceSettings.Command(
            cardId,
            request.baselineAmount(),
            toExpenseCategoryAmounts(request.limitCategoryPercents(), "Limit category percents must not be null"),
            request.salaryAmount(),
            request.bonusPercent()
        ));

        return ResponseEntity.noContent().build();
    }

    private PersonalFinanceCardId requireExistingCardId(String rawCardId) {
        PersonalFinanceCardId cardId = parseCardId(rawCardId);
        if (cardRepository.find(cardId).isEmpty()) {
            throw new PersonalFinanceCardNotFoundException("Personal finance card not found");
        }
        return cardId;
    }

    private static PersonalFinanceCardId parseCardId(String rawCardId) {
        String trimmed = rawCardId == null ? "" : rawCardId.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("cardId must not be blank");
        }

        try {
            return new PersonalFinanceCardId(UUID.fromString(trimmed));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("cardId must be a valid UUID");
        }
    }

    private static int validateYear(int year) {
        if (year < 1 || year > 9999) {
            throw new IllegalArgumentException("Year must be between 1 and 9999");
        }
        return year;
    }

    private static void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
    }

    private static Map<PersonalExpenseCategory, BigDecimal> toExpenseCategoryAmounts(
        Map<String, BigDecimal> rawCategoryAmounts,
        String nullMessage
    ) {
        if (rawCategoryAmounts == null) {
            throw new IllegalArgumentException(nullMessage);
        }

        Map<PersonalExpenseCategory, BigDecimal> result = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : rawCategoryAmounts.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("Expense category code must not be blank");
            }

            PersonalExpenseCategory category;
            try {
                category = PersonalExpenseCategory.valueOf(entry.getKey().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unsupported expense category: " + entry.getKey());
            }
            result.put(category, entry.getValue() == null ? BigDecimal.ZERO : entry.getValue());
        }
        return result;
    }

    private static PersonalFinanceSnapshotDto toDto(GetCardPersonalFinanceSnapshot.Result snapshot) {
        return new PersonalFinanceSnapshotDto(
            snapshot.cards().stream().map(PersonalFinanceController::toCardDto).toList(),
            toCardDto(snapshot.card()),
            snapshot.year(),
            snapshot.currency().getCurrencyCode(),
            snapshot.categories().stream()
                .map(category -> new ExpenseCategoryDto(
                    category.name(),
                    toCategoryLabel(category),
                    category.limitPeriod().name()
                ))
                .toList(),
            new ExpensesSectionDto(
                snapshot.expenses().months().stream()
                    .map(month -> new ExpenseMonthDto(
                        month.month(),
                        toStringAmountMap(month.actualCategoryAmounts()),
                        toStringAmountMap(month.limitCategoryAmounts()),
                        month.actualTotal().amount().toPlainString(),
                        month.limitTotal().amount().toPlainString()
                    ))
                    .toList(),
                toStringAmountMap(snapshot.expenses().actualTotalsByCategory()),
                toStringAmountMap(snapshot.expenses().limitTotalsByCategory()),
                snapshot.expenses().annualActualTotal().amount().toPlainString(),
                snapshot.expenses().annualLimitTotal().amount().toPlainString(),
                snapshot.expenses().averageMonthlyActualTotal().amount().toPlainString()
            ),
            new IncomeSectionDto(
                snapshot.income().months().stream()
                    .map(month -> new IncomeMonthDto(
                        month.month(),
                        month.totalAmount().amount().toPlainString(),
                        month.status() == null ? null : month.status().name()
                    ))
                    .toList(),
                snapshot.income().annualTotal().amount().toPlainString(),
                snapshot.income().averageMonthlyTotal().amount().toPlainString()
            ),
            new SettingsSectionDto(
                snapshot.settings().currentBalance().amount().toPlainString(),
                snapshot.settings().baselineAmount().amount().toPlainString(),
                toStringDecimalMap(snapshot.settings().limitCategoryPercents()),
                toStringAmountMap(snapshot.settings().limitCategoryAmounts()),
                snapshot.settings().monthlyLimitTotal().amount().toPlainString(),
                snapshot.settings().annualLimitTotal().amount().toPlainString(),
                toForecastDto(snapshot.settings().incomeForecast())
            )
        );
    }

    private static PersonalFinanceCardDto toCardDto(PersonalFinanceCard card) {
        return new PersonalFinanceCardDto(
            card.id().value().toString(),
            card.name(),
            card.createdAt().toString(),
            card.status().name()
        );
    }

    private static IncomeForecastDto toForecastDto(IncomeForecast forecast) {
        if (forecast == null) {
            return null;
        }

        return new IncomeForecastDto(
            forecast.salaryAmount().amount().toPlainString(),
            forecast.bonusPercent().toPlainString(),
            forecast.bonusAmount().amount().toPlainString(),
            forecast.totalAmount().amount().toPlainString()
        );
    }

    private static Map<String, String> toStringAmountMap(Map<PersonalExpenseCategory, Money> amounts) {
        return amounts.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().name(),
                entry -> entry.getValue().amount().toPlainString(),
                (left, right) -> right,
                LinkedHashMap::new
            ));
    }

    private static Map<String, String> toStringDecimalMap(Map<PersonalExpenseCategory, BigDecimal> values) {
        return values.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().name(),
                entry -> entry.getValue().toPlainString(),
                (left, right) -> right,
                LinkedHashMap::new
            ));
    }

    private static String toCategoryLabel(PersonalExpenseCategory category) {
        return switch (category) {
            case RESTAURANTS -> "Рестораны";
            case GROCERIES -> "Продукты";
            case PERSONAL -> "Личные";
            case UTILITIES -> "Коммунальные";
            case TRANSPORT -> "Транспорт";
            case GIFTS -> "Подарки";
            case INVESTMENTS -> "Инвестиции";
            case ENTERTAINMENT -> "Развлечения";
            case EDUCATION -> "Обучение";
        };
    }

    public record CreatePersonalFinanceCardRequest(String name) {}

    public record CreatePersonalFinanceCardResponse(String cardId) {}

    public record UpdatePersonalFinanceCardRequest(String name) {}

    public record UpdateMonthlyExpenseRequest(int year, Map<String, BigDecimal> categoryAmounts) {}

    public record UpdateMonthlyIncomeActualRequest(int year, BigDecimal totalAmount) {}

    public record UpdateCardSettingsRequest(
        BigDecimal baselineAmount,
        Map<String, BigDecimal> limitCategoryPercents,
        BigDecimal salaryAmount,
        BigDecimal bonusPercent
    ) {}

    public record PersonalFinanceSnapshotDto(
        List<PersonalFinanceCardDto> cards,
        PersonalFinanceCardDto card,
        int year,
        String currency,
        List<ExpenseCategoryDto> categories,
        ExpensesSectionDto expenses,
        IncomeSectionDto income,
        SettingsSectionDto settings
    ) {}

    public record PersonalFinanceCardDto(String id, String name, String createdAt, String status) {}

    public record ExpenseCategoryDto(String code, String label, String limitPeriod) {}

    public record ExpensesSectionDto(
        List<ExpenseMonthDto> months,
        Map<String, String> actualTotalsByCategory,
        Map<String, String> limitTotalsByCategory,
        String annualActualTotal,
        String annualLimitTotal,
        String averageMonthlyActualTotal
    ) {}

    public record ExpenseMonthDto(
        int month,
        Map<String, String> actualCategoryAmounts,
        Map<String, String> limitCategoryAmounts,
        String actualTotal,
        String limitTotal
    ) {}

    public record IncomeSectionDto(
        List<IncomeMonthDto> months,
        String annualTotal,
        String averageMonthlyTotal
    ) {}

    public record IncomeMonthDto(
        int month,
        String totalAmount,
        String status
    ) {}

    public record SettingsSectionDto(
        String currentBalance,
        String baselineAmount,
        Map<String, String> limitCategoryPercents,
        Map<String, String> limitCategoryAmounts,
        String monthlyLimitTotal,
        String annualLimitTotal,
        IncomeForecastDto incomeForecast
    ) {}

    public record IncomeForecastDto(
        String salaryAmount,
        String bonusPercent,
        String bonusAmount,
        String totalAmount
    ) {}
}
