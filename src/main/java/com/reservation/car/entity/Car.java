package com.reservation.car.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "car")
@Getter @Setter
@NoArgsConstructor
public class Car {

    @Id
    private UUID id;

    @Column(name = "make", nullable = false)
    private String make;

    @Column(name = "model", nullable = false)
    private String model;

    @Column(name = "license_plate", nullable = false, unique = true)
    private String licensePlate;

    // Constructor with parameters (excluding id for auto-generation)
    public Car(String make, String model, String licensePlate) {
        this.make = make;
        this.model = model;
        this.licensePlate = licensePlate;
    }

    // Constructor with all parameters including id (for test scenarios)
    public Car(UUID id, String make, String model, String licensePlate) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.licensePlate = licensePlate;
    }
}
