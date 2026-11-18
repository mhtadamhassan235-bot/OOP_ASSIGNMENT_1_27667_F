package com.auca.exceptions;

/** Custom checked exception of the AUCA_ID application (DuplicateRecordException). */
public class DuplicateRecordException extends Exception {
    private static final long serialVersionUID = 1L;

    public DuplicateRecordException(String message) {
        super(message);
    }

    public DuplicateRecordException(String message, Throwable cause) {
        super(message, cause);
    }
}
