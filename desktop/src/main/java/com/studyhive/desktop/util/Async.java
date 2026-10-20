package com.studyhive.desktop.util;

import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.concurrent.Task;

/**
 * Runs blocking work (every API call) off the JavaFX Application Thread, delivering the
 * result or error back on the FX thread via Task's onSucceeded/onFailed. Every controller
 * uses this instead of calling the API directly, so the UI never freezes on a network call.
 */
public final class Async {

    private Async() {
    }

    public static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException()));
        Thread thread = new Thread(task, "studyhive-async");
        thread.setDaemon(true);
        thread.start();
    }

    public static void run(Runnable work, Runnable onSuccess, Consumer<Throwable> onError) {
        run(
                () -> {
                    work.run();
                    return null;
                },
                v -> onSuccess.run(),
                onError);
    }
}
