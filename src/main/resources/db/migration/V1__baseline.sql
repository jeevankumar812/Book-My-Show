-- V1: baseline schema (matches JPA entities) + prod indexes/constraints.
-- Managed by Flyway. App uses ddl-auto=validate.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS cities (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  state VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(64),
  role VARCHAR(16) NOT NULL DEFAULT 'USER',
  created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS movies (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  genre VARCHAR(128),
  language VARCHAR(64),
  duration_minutes INT,
  rating DOUBLE PRECISION,
  release_date DATE,
  poster_url TEXT
);
CREATE INDEX IF NOT EXISTS idx_movies_genre ON movies(genre);
CREATE INDEX IF NOT EXISTS idx_movies_language ON movies(language);
CREATE INDEX IF NOT EXISTS idx_movies_title_trgm ON movies USING gin(title gin_trgm_ops);

CREATE TABLE IF NOT EXISTS theaters (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  address TEXT,
  city_id BIGINT NOT NULL REFERENCES cities(id)
);
CREATE INDEX IF NOT EXISTS idx_theaters_city ON theaters(city_id);

CREATE TABLE IF NOT EXISTS screens (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  total_seats INT,
  theater_id BIGINT NOT NULL REFERENCES theaters(id)
);
CREATE INDEX IF NOT EXISTS idx_screens_theater ON screens(theater_id);

CREATE TABLE IF NOT EXISTS seats (
  id BIGSERIAL PRIMARY KEY,
  seat_number VARCHAR(32) NOT NULL,
  seat_row VARCHAR(8),
  seat_col INT,
  seat_type VARCHAR(16),
  screen_id BIGINT NOT NULL REFERENCES screens(id)
);
CREATE INDEX IF NOT EXISTS idx_seats_screen ON seats(screen_id);

CREATE TABLE IF NOT EXISTS shows (
  id BIGSERIAL PRIMARY KEY,
  movie_id BIGINT NOT NULL REFERENCES movies(id),
  screen_id BIGINT NOT NULL REFERENCES screens(id),
  show_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME,
  ticket_price DOUBLE PRECISION
);
CREATE INDEX IF NOT EXISTS idx_shows_movie_date ON shows(movie_id, show_date);
CREATE INDEX IF NOT EXISTS idx_shows_screen_date ON shows(screen_id, show_date);

CREATE TABLE IF NOT EXISTS bookings (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  show_id BIGINT NOT NULL REFERENCES shows(id),
  total_price NUMERIC(10,2) NOT NULL,
  status VARCHAR(16) NOT NULL,
  idempotency_key UUID NOT NULL UNIQUE,
  version BIGINT NOT NULL DEFAULT 0,
  booked_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bookings_user ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_bookings_show ON bookings(show_id);

CREATE TABLE IF NOT EXISTS booking_seats (
  booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
  seat_id BIGINT NOT NULL REFERENCES seats(id),
  PRIMARY KEY (booking_id, seat_id)
);

-- Per-show seat inventory: the concurrency guard.
CREATE TABLE IF NOT EXISTS show_seats (
  id BIGSERIAL PRIMARY KEY,
  show_id BIGINT NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
  seat_id BIGINT NOT NULL REFERENCES seats(id) ON DELETE CASCADE,
  status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
  version BIGINT NOT NULL DEFAULT 0,
  hold_expires_at TIMESTAMPTZ,
  CONSTRAINT uq_show_seats_show_seat UNIQUE (show_id, seat_id)
);
CREATE INDEX IF NOT EXISTS idx_show_seats_show_status ON show_seats(show_id, status);
