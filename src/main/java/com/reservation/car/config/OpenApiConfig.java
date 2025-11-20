package com.reservation.car.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Provides comprehensive API documentation for the Car Reservation System.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI carReservationOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Car Reservation System API")
                .description("""
                    Enterprise-grade car reservation system API supporting 10K+ car fleets.
                    
                    ## Features
                    - **Fleet Management**: Efficient handling of large car inventories with pagination
                    - **Concurrent Reservations**: Thread-safe reservation creation with conflict prevention
                    - **Business Rules**: Automatic validation of reservation constraints (30-min cancellation)
                    
                    ## Authentication
                    All endpoints require the `X-User-ID` header for user identification and authorization.
                    
                    ## Rate Limits
                    - Maximum 200 items per page for performance
                    - Pagination required for large datasets
                    
                    ## Error Handling
                    Comprehensive error responses with detailed messages and error codes.
                    """)
                .version("1.0.0"));
    }
}