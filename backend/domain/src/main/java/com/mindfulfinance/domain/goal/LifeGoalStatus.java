package com.mindfulfinance.domain.goal;

public enum LifeGoalStatus {
    /**
     * The life goal is currently active and being worked towards.
     */
    ACTIVE,

    /**
     * The life goal has been achieved and the target amount has been reached.
     */
    ACHIEVED,

    /**
     * The life goal is no longer active and has been archived, either because it was abandoned or for record-keeping purposes.
     */
    ARCHIVED
}
