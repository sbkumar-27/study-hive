package com.studyhive.desktop.api;

import java.util.List;

public class ApiException extends RuntimeException {

    private final int status;
    private final List<String> fieldErrors;

    public ApiException(int status, String message, List<String> fieldErrors) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors;
    }

    public int getStatus() {
        return status;
    }

    public List<String> getFieldErrors() {
        return fieldErrors;
    }

    /** A message suitable for showing directly in a dialog/label. */
    public String displayMessage() {
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            return String.join("\n", fieldErrors);
        }
        return getMessage();
    }
}
