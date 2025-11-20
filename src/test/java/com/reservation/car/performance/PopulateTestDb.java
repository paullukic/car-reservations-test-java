package com.reservation.car.performance;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.UUID;

public class PopulateTestDb {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://localhost:5433/cardb";
        String user = "user";
        String password = "password";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            String sql = "INSERT INTO car (id, make, model, license_plate) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < 10000; i++) {
                    stmt.setObject(1, UUID.randomUUID());
                    stmt.setString(2, "Make" + (i % 10));
                    stmt.setString(3, "Model" + (i % 100));
                    stmt.setString(4, "LP" + i);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        }
        System.out.println("Populated test DB with 10,000 cars.");
    }
}