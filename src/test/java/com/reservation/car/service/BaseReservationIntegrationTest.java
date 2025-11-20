package com.reservation.car.service;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.reservation.car.entity.Car;
import com.reservation.car.repository.CarRepository;
import com.reservation.car.repository.ReservationRepository;
import com.reservation.car.util.TestDataFactory;

/**
 * Base class for integration tests that require database setup.
 * Provides common setup for repositories and a test car.
 */
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class BaseReservationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected ReservationService reservationService;

    @Autowired
    protected CarRepository carRepository;

    @Autowired
    protected ReservationRepository reservationRepository;

    protected Car testCar;

    @BeforeEach
    void setup() {
        reservationRepository.deleteAll();
        carRepository.deleteAll();
        
        testCar = TestDataFactory.createTestCar();
        carRepository.save(testCar);
    }
}