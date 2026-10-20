package com.studyhive.desktop.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Persists the JWT across app restarts in a config file under the user's profile, per
 * docs/architecture.md's "where each client stores the token" decision.
 */
public final class SessionStore {

    private static final Path CONFIG_DIR = Path.of(System.getProperty("user.home"), ".studyhive");
    private static final Path SESSION_FILE = CONFIG_DIR.resolve("session.properties");
    private static final String TOKEN_KEY = "token";

    public Optional<String> loadToken() {
        if (!Files.exists(SESSION_FILE)) {
            return Optional.empty();
        }
        Properties props = new Properties();
        try (var in = Files.newInputStream(SESSION_FILE)) {
            props.load(in);
        } catch (IOException e) {
            return Optional.empty();
        }
        String token = props.getProperty(TOKEN_KEY);
        return (token == null || token.isBlank()) ? Optional.empty() : Optional.of(token);
    }

    public void saveToken(String token) {
        Properties props = new Properties();
        props.setProperty(TOKEN_KEY, token);
        try {
            Files.createDirectories(CONFIG_DIR);
            try (var out = Files.newOutputStream(SESSION_FILE)) {
                props.store(out, "StudyHive desktop session -- do not share this file");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void clear() {
        try {
            Files.deleteIfExists(SESSION_FILE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
