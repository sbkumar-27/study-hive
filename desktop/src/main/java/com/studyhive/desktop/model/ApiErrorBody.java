package com.studyhive.desktop.model;

import java.time.Instant;
import java.util.List;

public record ApiErrorBody(
        Instant timestamp, int status, String error, String message, String path, List<String> fieldErrors) {
}
