package com.auca.exceptions;

/** Custom checked exception of the AUCA_ID application (BookUnavailableException). */
public class BookUnavailableException extends Exception {
    private static final long serialVersionUID = 1L;

    public BookUnavailableException(String message) {
        super(message);
    }

    public BookUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
