package com.reservation.car.performance;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.reservation.car.entity.Car;
import com.reservation.car.repository.CarRepository;

/**
 * Utility test used to populate the test PostgreSQL database with a large
 * number of Car records for load / Gatling performance testing. Run this test
 * against the test environment database before executing Gatling simulations.
 *
 * Usage (from host machine):
 *   ./mvnw test -Dtest=DatabasePopulationTest \
 *     -Dspring.profiles.active=test \
 *     -Dspring.datasource.url=jdbc:postgresql://localhost:5433/cardb \
 *     -Dspring.datasource.username=user \
 *     -Dspring.datasource.password=password
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Run manually when needed to populate test database")
public class DatabasePopulationTest {

    private static final int TARGET_COUNT = 10_000; // Number of cars to ensure exist

    @Autowired
    private CarRepository carRepository;

    @Test
    void populateCarsIfNeeded() {
        long existing = carRepository.count();
        if (existing >= TARGET_COUNT) {
            System.out.println("Database already has " + existing + " cars. Skipping population.");
            return;
        }

        int toCreate = (int) (TARGET_COUNT - existing);
        System.out.println("Creating " + toCreate + " car records...");
        List<Car> batch = new ArrayList<>(1000);
        int batchSize = 1000;
        for (int i = 0; i < toCreate; i++) {
            UUID id = UUID.randomUUID();
            String make = "Make" + (i % 25); // 25 distinct makes
            String model = "Model" + (i % 100); // 100 distinct models
            String licensePlate = "LOAD" + String.format("%05d", i) + UUID.randomUUID().toString().substring(0, 4);
            batch.add(new Car(id, make, model, licensePlate));
            if (batch.size() == batchSize) {
                carRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            carRepository.saveAll(batch);
        }
        long finalCount = carRepository.count();
        System.out.println("Population complete. Car count=" + finalCount);
    }
}
