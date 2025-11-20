package com.reservation.car.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reservation.car.util.TestDataFactory;

/**
 * Base class for controller tests providing common setup and utilities.
 * Subclasses must specify the controller to test using @WebMvcTest(ControllerClass.class)
 */
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    // Common test data - using fixed IDs for consistency
    protected final UUID testCarId = TestDataFactory.getTestCarId();
    protected final UUID testUserId = TestDataFactory.getTestUserId();
    protected final UUID testReservationId = TestDataFactory.getTestReservationId();
}