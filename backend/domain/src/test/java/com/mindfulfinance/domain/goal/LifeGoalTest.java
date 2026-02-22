package com.mindfulfinance.domain.goal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.mindfulfinance.domain.money.Money;
import static com.mindfulfinance.domain.shared.DomainErrorCode.LIFEGOAL_CREATED_AT_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.LIFEGOAL_ID_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.LIFEGOAL_STATUS_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.LIFEGOAL_TARGET_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO;
import static com.mindfulfinance.domain.shared.DomainErrorCode.LIFEGOAL_TARGET_DATE_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.LIFEGOAL_TITLE_NULL_OR_BLANK;
import com.mindfulfinance.domain.shared.DomainException;

public class LifeGoalTest {
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void testCreateLifeGoal() {
        // This test is a placeholder to ensure that the LifeGoal class can be instantiated without errors.
        // More comprehensive tests should be added to cover the behavior and constraints of the LifeGoal class.
        LifeGoal lifeGoal = new LifeGoal(
            LifeGoalId.random(),
            "Retirement Fund",
            new Money(new BigDecimal("100000.00"), Currency.getInstance("USD")),
            LocalDate.of(2040, 1, 1),
            LifeGoalStatus.ACTIVE,
            "Save for retirement by 2040",
            CREATED_AT
        );
        assertNotNull(lifeGoal);
    }

    @Test
    void testCreateLifeGoalWithBlankNotes() {
        LifeGoal lifeGoal = new LifeGoal(
            LifeGoalId.random(),
            "Vacation Fund",
            new Money(new BigDecimal("5000.00"), Currency.getInstance("USD")),
            LocalDate.of(2025, 6, 1),
            LifeGoalStatus.ACTIVE,
            "   ", // Blank notes should be normalized to null
            CREATED_AT
        );
        assertNotNull(lifeGoal);
        assertNull(lifeGoal.notes()); // Notes should be normalized to null
    }

    @Test
    void testCreateLifeGoalWithNonBlankNotes() {
        LifeGoal lifeGoal = new LifeGoal(
            LifeGoalId.random(),
            "Emergency Fund",
            new Money(new BigDecimal("10000.00"), Currency.getInstance("USD")),
            LocalDate.of(2023, 12, 31),
            LifeGoalStatus.ACTIVE,
            "Save for emergencies",
            CREATED_AT
        );
        assertNotNull(lifeGoal);
        assertEquals("Save for emergencies", lifeGoal.notes()); // Notes should be preserved
    }

    @Test
    void testCreateLifeGoalWithBlankTitle() {
        DomainException exception = assertThrows(DomainException.class, () -> new LifeGoal(
                LifeGoalId.random(),
                "   ", // Blank title should cause an exception
                new Money(new BigDecimal("10000.00"), Currency.getInstance("USD")),
                LocalDate.of(2023, 12, 31),
                LifeGoalStatus.ACTIVE,
                "Save for emergencies",
                CREATED_AT
            )
        );
        assertEquals(LIFEGOAL_TITLE_NULL_OR_BLANK, exception.code());
    }

    @Test
    void testCreateLifeGoalWithNullTitle() {
        DomainException exception = assertThrows(DomainException.class, () -> new LifeGoal(
                LifeGoalId.random(),
                null, // Null title should cause an exception
                new Money(new BigDecimal("10000.00"), Currency.getInstance("USD")),
                LocalDate.of(2023, 12, 31),
                LifeGoalStatus.ACTIVE,
                "Save for emergencies",
                CREATED_AT
            )
        );
        assertEquals(LIFEGOAL_TITLE_NULL_OR_BLANK, exception.code());
    }   

    @Test
    void testIsActive() {
        LifeGoal activeGoal = new LifeGoal(
            LifeGoalId.random(),
            "Active Goal",
            new Money(new BigDecimal("10000.00"), Currency.getInstance("USD")),
            LocalDate.of(2023, 12, 31),
            LifeGoalStatus.ACTIVE,
            "This goal is active",
            CREATED_AT
        );
        assertNotNull(activeGoal);
        assertTrue(activeGoal.isActive());
    }

    @Test
    void testIsNotActive() {
        LifeGoal achievedGoal = new LifeGoal(
            LifeGoalId.random(),
            "Achieved Goal",
            new Money(new BigDecimal("10000.00"), Currency.getInstance("USD")),
            LocalDate.of(2023, 12, 31),
            LifeGoalStatus.ACHIEVED,
            "This goal is achieved",
            CREATED_AT
        );
        assertNotNull(achievedGoal);
        assertFalse(achievedGoal.isActive());
    }

    @Test
    void testIsArchived() {
        LifeGoal archivedGoal = new LifeGoal(
            LifeGoalId.random(),
            "Archived Goal",
            new Money(new BigDecimal("10000.00"), Currency.getInstance("USD")),
            LocalDate.of(2023, 12, 31),
            LifeGoalStatus.ARCHIVED,
            "This goal is archived",
            CREATED_AT
        );
        assertNotNull(archivedGoal);
        assertFalse(archivedGoal.isActive());
    }

    @Test
    void testTrimTitleAndNotes() {
        LifeGoal lifeGoal = new LifeGoal(
            LifeGoalId.random(),
            "  Trimmed Title  ", // Title should be trimmed
            new Money(new BigDecimal("10000.00"), Currency.getInstance("USD")),
            LocalDate.of(2023, 12, 31),
            LifeGoalStatus.ACTIVE,
            "  Trimmed Notes  ", // Notes should be trimmed
            CREATED_AT
        );
        assertNotNull(lifeGoal);
        assertEquals("Trimmed Title", lifeGoal.title()); // Title should be trimmed
        assertEquals("Trimmed Notes", lifeGoal.notes()); // Notes should be trimmed
    }

    @Test
    void testNullNotes() {
        LifeGoal lifeGoal = new LifeGoal(
            LifeGoalId.random(),
            "Goal with Null Notes",
            new Money(new BigDecimal("10000.00"), Currency.getInstance("USD")),
            LocalDate.of(2023, 12, 31),
            LifeGoalStatus.ACTIVE,
            null, // Notes can be null
            CREATED_AT
        );
        assertNotNull(lifeGoal);
        assertNull(lifeGoal.notes()); // Notes should be null
    }

    @Test
    void testIdCannotBeNull() {
        DomainException exception = assertThrows(DomainException.class, () -> new LifeGoal(
                null, // Null id should cause an exception
                "Valid Title",
                new Money(new BigDecimal("10000.00"), Currency.getInstance("USD")),
                LocalDate.of(2023, 12, 31),
                LifeGoalStatus.ACTIVE,
                "Valid notes",
                CREATED_AT
            )
        );
        assertEquals(LIFEGOAL_ID_NULL, exception.code());
    }

    @Test
    void testTargetAmountCannotBeNull() {
        DomainException exception = assertThrows(DomainException.class, () -> new LifeGoal(
                LifeGoalId.random(),
                "Valid Title",
                null, // Null targetAmount should cause an exception
                LocalDate.of(2023, 12, 31),
                LifeGoalStatus.ACTIVE,  
                "Valid notes",
                CREATED_AT
            )
        );
        assertEquals(LIFEGOAL_TARGET_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO, exception.code());
    }

    @Test
    void testTargetAmountCannotBeNegative() {
        DomainException exception = assertThrows(DomainException.class, () -> new LifeGoal(
                LifeGoalId.random(),
                "Valid Title",
                new Money(new BigDecimal("-100.00"), Currency.getInstance("USD")), // Negative targetAmount should cause an exception
                LocalDate.of(2023, 12, 31),
                LifeGoalStatus.ACTIVE,
                "Valid notes",
                CREATED_AT
            )
        );
        assertEquals(LIFEGOAL_TARGET_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO, exception.code());
    }

    @Test
    void testTargetAmountCannotBeZero() {
        DomainException exception = assertThrows(DomainException.class, () -> new LifeGoal(
                LifeGoalId.random(),
                "Valid Title",
                new Money(BigDecimal.ZERO, Currency.getInstance("USD")), // Zero targetAmount should cause an exception
                LocalDate.of(2023, 12, 31),
                LifeGoalStatus.ACTIVE,      
                "Valid notes",
                CREATED_AT
            )
        );  
        assertEquals(LIFEGOAL_TARGET_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO, exception.code());
    }

    @Test
    void testTargetDateCannotBeNull() {
        DomainException exception = assertThrows(DomainException.class, () -> new LifeGoal(
                LifeGoalId.random(),
                "Valid Title",
                new Money(new BigDecimal("10000.00"), Currency.getInstance("USD")),
                null, // Null targetDate should cause an exception
                LifeGoalStatus.ACTIVE,
                "Valid notes",
                CREATED_AT
            )
        );
        assertEquals(LIFEGOAL_TARGET_DATE_NULL, exception.code());  
    }

    @Test
    void testStatusCannotBeNull() {
        DomainException exception = assertThrows(DomainException.class, () -> new LifeGoal(
                LifeGoalId.random(),
                "Valid Title",
                new Money(new BigDecimal("10000.00"), Currency.getInstance("USD")),
                LocalDate.of(2023, 12, 31),
                null, // Null status should cause an exception
                "Valid notes",
                CREATED_AT
            )
        );
        assertEquals(LIFEGOAL_STATUS_NULL, exception.code());
    }

    @Test
    void testCreatedAtCannotBeNull() {
        DomainException exception = assertThrows(DomainException.class, () -> new LifeGoal(
                LifeGoalId.random(),
                "Valid Title",
                new Money(new BigDecimal("10000.00"), Currency.getInstance("USD")),
                LocalDate.of(2023, 12, 31),
                LifeGoalStatus.ACTIVE,
                "Valid notes",
                null // Null createdAt should cause an exception
            )
        );
        assertEquals(LIFEGOAL_CREATED_AT_NULL, exception.code());
    }
}
