package ru.funduruk.lunfyServer.util;

import java.security.SecureRandom;

public class CodeGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String sixDigit() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}