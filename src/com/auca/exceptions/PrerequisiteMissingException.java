package com.auca.exceptions;

/** Custom checked exception of the AUCA_ID application (PrerequisiteMissingException). */
public class PrerequisiteMissingException extends Exception {
    private static final long serialVersionUID = 1L;

    public PrerequisiteMissingException(String message) {
        super(message);
    }

    public PrerequisiteMissingException(String message, Throwable cause) {
        super(message, cause);
    }
}
