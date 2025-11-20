package com.reservation.car.service;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.reservation.car.dto.response.CarResponseDTO;
import com.reservation.car.entity.Car;
import com.reservation.car.repository.CarRepository;
import com.reservation.car.util.TestDataFactory;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CarServiceImpl carService;

    private Car car1, car2;
    private Page<Car> carPage;

    @BeforeEach
    void setUp() {
        car1 = TestDataFactory.createTestCar(TestDataFactory.getTestCarId(), "Tesla", "Model 3", "ABC-123");
        car2 = TestDataFactory.createTestCar(TestDataFactory.getTestCarId2(), "BMW", "X5", "XYZ-456");
        carPage = new PageImpl<>(List.of(car1, car2));
    }

    @Test
    void shouldReturnPagedResponse_whenGetAllCars() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("make", "model"));
        when(carRepository.findAllCarsPaginated(pageable)).thenReturn(carPage);

        // Act
        Page<CarResponseDTO> response = carService.getAllCars(0, 10);

        // Assert
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent().get(0).getMake()).isEqualTo("Tesla");
    }

    @Test
    void shouldReturnPagedResponse_whenFindAvailableCars() {
        // Arrange
        Instant start = Instant.now().plusSeconds(3600); // 1 hour in future
        Instant end = start.plusSeconds(7200); // 2 hours total
        Pageable pageable = PageRequest.of(0, 10, Sort.by("make", "model"));
        when(carRepository.findAvailableCarsForTimePeriod(start, end, pageable)).thenReturn(carPage);

        // Act
        Page<CarResponseDTO> response = carService.findAvailableCars(start, end, 0, 10);

        // Assert
        assertThat(response.getContent()).hasSize(2);
    }
}