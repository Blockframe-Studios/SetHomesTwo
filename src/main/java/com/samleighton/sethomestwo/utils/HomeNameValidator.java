package com.samleighton.sethomestwo.utils;

/**
 * Shape validation for home names. Deliberately free of any Bukkit dependency
 * so it can be unit tested without a server harness. Uniqueness is not checked
 * here; that requires the database and lives on HomesDao.nameExists.
 */
public final class HomeNameValidator {

    public enum Result {
        VALID,
        EMPTY,
        TOO_LONG
    }

    private HomeNameValidator() {
    }

    /**
     * Trim a raw name into its stored form.
     *
     * @param rawName The name as typed by the player
     * @return The trimmed name, or an empty string when the input was null
     */
    public static String normalise(String rawName) {
        if (rawName == null) return "";
        return rawName.trim();
    }

    /**
     * @param rawName   The name as typed by the player
     * @param maxLength The configured maximum length
     * @return VALID, or the reason the name was rejected
     */
    public static Result validate(String rawName, int maxLength) {
        String name = normalise(rawName);

        if (name.isEmpty()) return Result.EMPTY;
        if (name.length() > maxLength) return Result.TOO_LONG;

        return Result.VALID;
    }
}
