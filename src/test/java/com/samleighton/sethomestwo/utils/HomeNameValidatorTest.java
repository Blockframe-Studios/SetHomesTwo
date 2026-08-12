package com.samleighton.sethomestwo.utils;

import com.samleighton.sethomestwo.utils.HomeNameValidator.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeNameValidatorTest {

    @Test
    void normaliseTurnsNullIntoEmptyString() {
        assertEquals("", HomeNameValidator.normalise(null));
    }

    @Test
    void normaliseTrimsSurroundingWhitespace() {
        assertEquals("base camp", HomeNameValidator.normalise("  base camp  "));
    }

    @Test
    void blankNameIsEmpty() {
        assertEquals(Result.EMPTY, HomeNameValidator.validate("   ", 32));
    }

    @Test
    void nullNameIsEmpty() {
        assertEquals(Result.EMPTY, HomeNameValidator.validate(null, 32));
    }

    @Test
    void nameAtExactlyMaxLengthIsValid() {
        assertEquals(Result.VALID, HomeNameValidator.validate("a".repeat(32), 32));
    }

    @Test
    void nameOneOverMaxLengthIsTooLong() {
        assertEquals(Result.TOO_LONG, HomeNameValidator.validate("a".repeat(33), 32));
    }

    @Test
    void lengthIsMeasuredAfterTrimming() {
        // 32 characters plus padding trims back under the limit.
        assertEquals(Result.VALID, HomeNameValidator.validate("  " + "a".repeat(32) + "  ", 32));
    }
}
