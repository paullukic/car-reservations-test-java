package com.reservation.car;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"spring.flyway.baseline-on-migrate=true"})
@Disabled("Requires PostgreSQL database - run with Docker: docker-compose up -d")
class CarApplicationTests {

	@Test
	void contextLoads() {
	}

}
