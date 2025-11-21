-- 01_init.sql

-- Install extension for GIST indexing on btree operators (needed for the exclusion constraint)
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Car Table
CREATE TABLE car (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    make VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    license_plate VARCHAR(20) UNIQUE NOT NULL
);

-- Reservation Table
CREATE TABLE reservation (
    id UUID PRIMARY KEY,
    car_id UUID NOT NULL REFERENCES car(id),
    user_id UUID NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),

    -- BUSINESS VALIDATION: Reservations must be between 2 and 24 hours (7200s and 86400s)
    CONSTRAINT reservation_duration_check CHECK (
        EXTRACT(EPOCH FROM (end_time - start_time)) BETWEEN 7200 AND 86400
    ),
    
    CONSTRAINT reservation_future_check CHECK (
        start_time > NOW()
    ),
    
    CONSTRAINT reservation_time_order_check CHECK (
        end_time > start_time
    ),
    
    -- SAFETY NET: Database-level exclusion constraint
    -- Database doesn't do heavy lifting on every request
    -- Primary validation happens in application layer (ReservationServiceImpl)
    -- This is a last line of defense against double bookings 
    -- constraint that catches rare race conditions where multiple requests
    -- pass application validation simultaneously eg. 10,000 concurrent requests
    -- which is highly unlikely.
    CONSTRAINT no_double_booking
    EXCLUDE USING GIST (
        car_id WITH =,
        tstzrange(start_time, end_time, '()') WITH && -- '()' ensures exclusive upper bound (start=10, end=11 does not overlap with start=11, end=12)
    )
    WHERE (status = 'CONFIRMED')
);

-- Performance indexes for the reservation system
-- Essential for checking car availability (car_id + time range + status)
CREATE INDEX idx_reservation_car_time_status ON reservation(car_id, start_time, end_time, status);

-- Essential for car listing API pagination (make, model ordering)
CREATE INDEX idx_car_make_model ON car(make, model);

-- Sample data for testing
INSERT INTO car (make, model, license_plate) VALUES 
    ('Tesla', 'Model S', 'TSL-001'),
    ('Tesla', 'Model 3', 'TSL-002'),
    ('BMW', 'X5', 'BMW-001'),
    ('BMW', 'X3', 'BMW-002'),
    ('Audi', 'A4', 'AUD-001'),
    ('Audi', 'Q7', 'AUD-002'),
    ('Mercedes', 'C-Class', 'MER-001'),
    ('Mercedes', 'GLE', 'MER-002'),
    ('Volkswagen', 'Golf', 'VW-001'),
    ('Volkswagen', 'Passat', 'VW-002');
