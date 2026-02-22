package com.mindfulfinance.domain.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

public class PreconditionsTest {
    @Test
    void requireNonBlank_validString_returnsTrimmedString() {
        String result = Preconditions.requireNonBlank("  valid string  ", "testField");
        assertEquals("valid string", result);
    }
    
    @Test
    void requireNonBlank_nullString_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            Preconditions.requireNonBlank(null, "testField")
        );
        assertEquals("testField must not be null or blank", exception.getMessage());
    }
    
    @Test
    void requireNonBlank_blankString_throwsException() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> 
            Preconditions.requireNonBlank("   ", "testField")
        );
        assertEquals("testField must not be null or blank", exception.getMessage());
    }
}
