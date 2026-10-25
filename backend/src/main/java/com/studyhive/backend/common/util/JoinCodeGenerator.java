package com.studyhive.backend.common.util;

import java.security.SecureRandom;

public final class JoinCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I, less ambiguous
    private static final int LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private JoinCodeGenerator() {
    }

    public static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
