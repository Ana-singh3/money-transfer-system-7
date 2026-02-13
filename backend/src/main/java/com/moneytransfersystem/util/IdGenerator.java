package com.moneytransfersystem.util;

import java.util.UUID;

public final class IdGenerator {

    private IdGenerator() {
        // Utility class
    }

    public static String generateAccountId() {
        return "ACC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    public static String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}

