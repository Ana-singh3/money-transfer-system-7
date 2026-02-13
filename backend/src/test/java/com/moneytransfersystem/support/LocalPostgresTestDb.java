package com.moneytransfersystem.support;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Utilities for tests that use a real local PostgreSQL instance (no Docker/Testcontainers).
 * Ensures the required test database exists before Spring Boot tries to connect.
 */
public final class LocalPostgresTestDb {

    private LocalPostgresTestDb() {}

    public static void ensureDatabaseExists(
            String host,
            int port,
            String adminDatabase,
            String username,
            String password,
            String databaseToCreate
    ) {
        String adminUrl = "jdbc:postgresql://" + host + ":" + port + "/" + adminDatabase;

        try (Connection conn = DriverManager.getConnection(adminUrl, username, password)) {
            boolean exists;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM pg_database WHERE datname = ?"
            )) {
                ps.setString(1, databaseToCreate);
                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }

            if (!exists) {
                try (Statement st = conn.createStatement()) {
                    st.execute("CREATE DATABASE \"" + databaseToCreate + "\"");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to ensure local PostgreSQL database exists. " +
                            "Tried admin URL=" + adminUrl +
                            ", dbToCreate=" + databaseToCreate +
                            ", username=" + username +
                            ". Root cause: " + e.getMessage(),
                    e
            );
        }
    }
}


