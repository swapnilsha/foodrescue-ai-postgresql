package com.foodrescue;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CleanupDb {

    @Test
    public void dropTestDatabase() throws Exception {
        System.out.println("=== CLEANING UP TEST DATABASE ===");
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", "root", "sakharam");
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DROP DATABASE IF EXISTS foodrescue_test");
            System.out.println("=== TEST DATABASE DROPPED SUCCESSFULLY ===");
        } catch (Exception e) {
            System.err.println("Failed to drop database: " + e.getMessage());
            throw e;
        }
    }
}
