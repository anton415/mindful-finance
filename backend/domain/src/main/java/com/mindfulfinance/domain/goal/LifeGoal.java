package com.mindfulfinance.domain.goal;

import java.time.Instant;
import java.time.LocalDate;

import com.mindfulfinance.domain.money.Money;
import static com.mindfulfinance.domain.shared.DomainErrorCode.LIFEGOAL_CREATED_AT_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.LIFEGOAL_ID_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.LIFEGOAL_STATUS_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.LIFEGOAL_TARGET_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO;
import static com.mindfulfinance.domain.shared.DomainErrorCode.LIFEGOAL_TARGET_DATE_NULL;
import static com.mindfulfinance.domain.shared.DomainErrorCode.LIFEGOAL_TITLE_NULL_OR_BLANK;
import com.mindfulfinance.domain.shared.DomainException;

/**
 * Represents a life goal that a user is working towards, such as saving for a down payment on a house or funding a child's education.
 * This class is immutable and thread-safe.
 */
public record LifeGoal(
  LifeGoalId id,
  String title,
  Money targetAmount,
  LocalDate targetDate,
  LifeGoalStatus status,
  String notes,
  Instant createdAt
) {
    /**
     * Constructs a new LifeGoal with the given parameters.
     * @param id the unique identifier for the LifeGoal, must not be null
     * @param title the title or name of the life goal, must not be null or blank
     * @param targetAmount the target amount of money needed to achieve the life goal, must not be null, negative, or zero
     * @param targetDate the target date by which the life goal should be achieved, must not be null
     * @param status the current status of the life goal (e.g., ACTIVE, ACHIEVED, ARCHIVED), must not be null
     * @param notes optional notes or description about the life goal, can be null or blank
     * @param createdAt the timestamp when the life goal was created, must not be null
     */
    public LifeGoal {
        if (title != null) {
            title = title.trim(); // Trim non-blank title
        }
        if (notes != null) {
            if (notes.isBlank()) {
                notes = null; // Normalize blank notes to null
            } else {
                notes = notes.trim(); // Trim non-blank notes
            }
        }

        if (id == null) {
            throw new DomainException(LIFEGOAL_ID_NULL, "LifeGoal id cannot be null", null);
        }
        if (title == null || title.isBlank()) {
            throw new DomainException(LIFEGOAL_TITLE_NULL_OR_BLANK, "LifeGoal title cannot be null or blank", null);
        }
        if (targetAmount == null || targetAmount.isNegative() || targetAmount.isZero()) {
            throw new DomainException(LIFEGOAL_TARGET_AMOUNT_NULL_OR_NEGATIVE_OR_ZERO, "LifeGoal targetAmount cannot be null, negative, or zero", null);
        }
        if (targetDate == null) {
            throw new DomainException(LIFEGOAL_TARGET_DATE_NULL, "LifeGoal targetDate cannot be null", null);
        }
        if (status == null) {
            throw new DomainException(LIFEGOAL_STATUS_NULL, "LifeGoal status cannot be null", null);
        }
        if (createdAt == null) {
            throw new DomainException(LIFEGOAL_CREATED_AT_NULL, "LifeGoal createdAt cannot be null", null);
        }
    }

    /**
     * Checks if the life goal is currently active.
     * @return true if the life goal status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return status == LifeGoalStatus.ACTIVE;
    }
}
