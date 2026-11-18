package com.auca.exceptions;

/** Custom checked exception of the AUCA_ID application (RecordNotFoundException). */
public class RecordNotFoundException extends Exception {
    private static final long serialVersionUID = 1L;

    public RecordNotFoundException(String message) {
        super(message);
    }

    public RecordNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
