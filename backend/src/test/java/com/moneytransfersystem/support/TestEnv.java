package com.moneytransfersystem.support;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Small helper to read key/value settings for tests.
 *
 * Resolution order:
 * 1) `config/env.properties` (repo root, gitignored)
 * 2) OS environment variables
 * 3) provided default
 */
public final class TestEnv {

    private static final Properties FILE_PROPS = loadOptional("config/env.properties");

    private TestEnv() {}

    public static String get(String key, String defaultValue) {
        String v = FILE_PROPS.getProperty(key);
        if (v != null && !v.isBlank()) return v;

        v = System.getenv(key);
        if (v != null && !v.isBlank()) return v;

        return defaultValue;
    }

    private static Properties loadOptional(String relativePath) {
        Properties props = new Properties();
        try {
            Path p = Path.of(relativePath);
            if (!Files.exists(p)) return props;
            try (InputStream in = Files.newInputStream(p)) {
                props.load(in);
            }
        } catch (Exception ignored) {
            // best-effort: tests can still run using env vars/defaults
        }
        return props;
    }
}


