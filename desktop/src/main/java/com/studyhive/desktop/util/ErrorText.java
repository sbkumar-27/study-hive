package com.studyhive.desktop.util;

import com.studyhive.desktop.api.ApiException;

public final class ErrorText {

    private ErrorText() {
    }

    public static String of(Throwable error) {
        if (error instanceof ApiException apiException) {
            return apiException.displayMessage();
        }
        if (error != null && error.getCause() instanceof ApiException apiException) {
            return apiException.displayMessage();
        }
        return "Could not reach the server. Is the backend running?";
    }
}
