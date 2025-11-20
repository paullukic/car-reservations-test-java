package com.reservation.car.performance

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class CarReservationLoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8081") // Test app on 8081
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // Scenario 1: List cars with pagination (simulate browsing)
  val listCarsScenario = scenario("List Cars")
    .exec(http("List Cars Page 1")
      .get("/api/v1/cars?page=0&size=20")
      .check(status.is(200)))
    .pause(1.second)
    .exec(http("List Cars Page 2")
      .get("/api/v1/cars?page=1&size=20")
      .check(status.is(200)))

  // Scenario 2: Check availability
  val checkAvailabilityScenario = scenario("Check Availability")
    .exec(http("Check Available Cars")
      .get("/api/v1/cars/available?startTime=2025-11-20T10:00:00Z&endTime=2025-11-20T12:00:00Z")
      .check(status.is(200)))

  // Scenario 3: Create reservation (with concurrency test)
  // First fetch a real car ID, then create reservation
  val createReservationScenario = scenario("Create Reservation")
    .exec(http("Fetch Cars for Reservation")
      .get("/api/v1/cars?page=0&size=1")
      .check(status.is(200))
      .check(jsonPath("$.content[0].id").saveAs("carId"))) // Extract first car ID
    .exec(http("Create Reservation")
      .post("/api/v1/reservations")
      .header("X-User-ID", "550e8400-e29b-41d4-a716-446655440000") // Sample user ID
      .body(StringBody("""{"carId": "#{carId}", "userId": "550e8400-e29b-41d4-a716-446655440000", "startTime": "2025-11-20T10:00:00Z", "endTime": "2025-11-20T12:00:00Z"}"""))
      .check(status.in(201, 409))) // 201 success, 409 conflict

  // Setup: Ramp up users over time
  setUp(
    listCarsScenario.inject(
      rampUsers(100).during(30.seconds), // 100 users over 30s for listing
      constantUsersPerSec(10).during(60.seconds) // Steady load
    ),
    checkAvailabilityScenario.inject(
      rampUsers(50).during(30.seconds)
    ),
    createReservationScenario.inject(
      rampUsers(20).during(30.seconds), // Simulate concurrent bookings of 20 users over 30 seconds
      constantUsersPerSec(5).during(60.seconds)
    )
  ).protocols(httpProtocol)
}