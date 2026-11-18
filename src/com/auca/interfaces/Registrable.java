package com.auca.interfaces;

import com.auca.exceptions.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


public interface Registrable {

    int MIN_NAME_LENGTH = 2;
    int MAX_NAME_LENGTH = 60;
    int MAX_EMAIL_LENGTH = 100;
    int MIN_PHONE_LENGTH = 10;
    int MAX_PHONE_LENGTH = 15;
    String DATE_PATTERN = "yyyy-MM-dd";

    void validate() throws ValidationException;

    void printDetails();

    String getRegistrationSummary();


    static String requireText(String label, String value, int min, int max) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(label + " is required and cannot be blank.");
        }
        if (!value.equals(value.trim())) {
            throw new ValidationException(label + " must not start or end with spaces.");
        }
        if (value.length() < min || value.length() > max) {
            throw new ValidationException(label + " must be " + min + "-" + max + " characters long (got " + value.length() + ").");
        }
        return value;
    }

    static String requireName(String label, String value) throws ValidationException {
        requireText(label, value, MIN_NAME_LENGTH, MAX_NAME_LENGTH);
        if (!value.matches("^[\\p{L}][\\p{L} .'-]*$")) {
            throw new ValidationException(label + " may contain only letters, spaces, dots, apostrophes and hyphens.");
        }
        return value;
    }

    static String requireId(String label, String value) throws ValidationException {
        requireText(label, value, Identifiable.MIN_ID_LENGTH, Identifiable.MAX_ID_LENGTH);
        if (!value.matches("^[A-Za-z0-9._-]+$")) {
            throw new ValidationException(label + " may contain only letters, digits, '.', '_' and '-' (no spaces).");
        }
        return value;
    }


    static String requireCode(String label, String value, int min, int max) throws ValidationException {
        requireText(label, value, min, max);
        if (!value.matches("^[A-Za-z0-9][A-Za-z0-9 ._-]*$")) {
            throw new ValidationException(label + " may contain only letters, digits, spaces, '.', '_' and '-'.");
        }
        return value;
    }

    static String requireEmail(String label, String value) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(label + " is required and cannot be blank.");
        }
        if (value.length() > MAX_EMAIL_LENGTH) {
            throw new ValidationException(label + " must not exceed " + MAX_EMAIL_LENGTH + " characters.");
        }
        if (!value.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$") || value.contains("..")) {
            throw new ValidationException(label + " is not a valid email address (example: alice@example.com).");
        }
        return value;
    }

    
    static String requirePhone(String label, String value) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(label + " is required and cannot be blank.");
        }
        if (!value.matches("^\\+?\\d{" + MIN_PHONE_LENGTH + "," + MAX_PHONE_LENGTH + "}$")) {
            throw new ValidationException(label + " must contain " + MIN_PHONE_LENGTH + "-" + MAX_PHONE_LENGTH
                    + " digits with an optional leading + (no spaces or symbols).");
        }
        return value;
    }


    static LocalDate parseDate(String raw) throws ValidationException {
        try {
            return LocalDate.parse(raw.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date: use YYYY-MM-DD (example: 2004-05-16).", e);
        }
    }

    static void requireNotNull(String label, Object value) throws ValidationException {
        if (value == null) {
            throw new ValidationException(label + " is required (no record selected).");
        }
    }


    static void requirePastDate(String label, LocalDate date) throws ValidationException {
        requireNotNull(label, date);
        if (!date.isBefore(LocalDate.now())) {
            throw new ValidationException(label + " must be in the past.");
        }
        if (date.isBefore(LocalDate.of(1900, 1, 1))) {
            throw new ValidationException(label + " cannot be before 1900-01-01.");
        }
    }


    static void requireNotFuture(String label, LocalDate date) throws ValidationException {
        requireNotNull(label, date);
        if (date.isAfter(LocalDate.now())) {
            throw new ValidationException(label + " cannot be in the future.");
        }
        if (date.isBefore(LocalDate.of(1900, 1, 1))) {
            throw new ValidationException(label + " cannot be before 1900-01-01.");
        }
    }

    static void requireRange(String label, int value, int min, int max) throws ValidationException {
        if (value < min || value > max) {
            throw new ValidationException(label + " must be between " + min + " and " + max + " (got " + value + ").");
        }
    }


    static void requireMoney(String label, BigDecimal value) throws ValidationException {
        requireNotNull(label, value);
        if (value.signum() <= 0) {
            throw new ValidationException(label + " must be strictly positive.");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new ValidationException(label + " cannot have more than 2 decimal places.");
        }
        if (value.compareTo(new BigDecimal("1000000000")) > 0) {
            throw new ValidationException(label + " is unrealistically large (max 1,000,000,000).");
        }
    }
}
