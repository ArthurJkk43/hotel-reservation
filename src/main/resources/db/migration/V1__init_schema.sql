
-- =====================================================================
-- Hotel Reservation Platform: database schema v3 (PostgreSQL 14+)
-- Supersedes schema_v2.sql
--
-- DESIGN PRINCIPLES
--   * Money is NUMERIC(12,2) + ISO currency code. Never FLOAT.
--   * Timestamps are TIMESTAMPTZ (UTC). Stay dates are DATE (hotel-local).
--   * Enum-like columns are VARCHAR + CHECK, mapped with
--     @Enumerated(EnumType.STRING). Easier to migrate than native ENUMs.
--   * Soft delete (deleted_at) only where history matters (users, hotels).
--   * Business invariants live in the DB where possible (exclusion
--     constraints, CHECKs, composite FKs), not only in Java.
--   * Prices and policies are SNAPSHOTTED onto the reservation, so later
--     changes never rewrite history.
--   * MULTI-TENANCY: shared schema, hotel_id as the tenant key. Every
--     hotel-owned row carries hotel_id, and composite FKs (child_id,
--     hotel_id) -> parent(id, hotel_id) make it impossible for a row of
--     hotel A to point at a row of hotel B. WHO may act on a hotel is
--     decided by hotel_staff and enforced in the service layer.
--
-- SECTIONS
--   0 Extensions + helpers      4 Pricing + promotions
--   1 Identity, roles, tokens   5 Reservations + payments
--   2 Hotels + staff            6 Reviews + favorites
--   3 Room types, rooms, stock  7 Audit log + seed data
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. EXTENSIONS + HELPERS
-- ---------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS btree_gist;   -- exclusion constraints on (id, daterange)
CREATE EXTENSION IF NOT EXISTS citext;       -- case-insensitive email
CREATE EXTENSION IF NOT EXISTS pg_trgm;      -- fuzzy search on hotel name / city

CREATE OR REPLACE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- 1. IDENTITY, ROLES, TOKENS
-- ---------------------------------------------------------------------
-- Platform-level roles: GUEST (default), ADMIN (platform operator).
-- Hotel-level roles (OWNER / MANAGER / RECEPTIONIST) live in hotel_staff,
-- because "manager" only makes sense relative to a specific hotel.
CREATE TABLE role (
    id          SMALLINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(30) NOT NULL UNIQUE,          -- 'GUEST', 'ADMIN'
    description VARCHAR(255)
);

CREATE TABLE app_user (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email                 CITEXT       NOT NULL UNIQUE,
    password_hash         VARCHAR(100) NOT NULL,       -- BCrypt/Argon2, never plain text
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100) NOT NULL,
    phone                 VARCHAR(30),
    preferred_language    CHAR(2)      NOT NULL DEFAULT 'en',
    status                VARCHAR(25)  NOT NULL DEFAULT 'PENDING_VERIFICATION'
                          CHECK (status IN ('PENDING_VERIFICATION','ACTIVE','LOCKED','DISABLED')),
    email_verified_at     TIMESTAMPTZ,
    failed_login_attempts SMALLINT     NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,                 -- brute-force protection
    last_login_at         TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at            TIMESTAMPTZ                  -- soft delete (GDPR: anonymize later)
);

CREATE TABLE user_role (
    user_id     BIGINT   NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id     SMALLINT NOT NULL REFERENCES role(id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

-- Refresh tokens for JWT sessions. Store a HASH of the token, not the token.
CREATE TABLE refresh_token (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    token_hash  CHAR(64)     NOT NULL UNIQUE,         -- SHA-256 hex
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,                          -- logout / rotation
    user_agent  VARCHAR(255),
    ip_address  INET,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);

-- Single-use tokens: e-mail verification, password reset.
CREATE TABLE one_time_token (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    type        VARCHAR(30)  NOT NULL CHECK (type IN ('EMAIL_VERIFICATION','PASSWORD_RESET')),
    token_hash  CHAR(64)     NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_one_time_token_user ON one_time_token(user_id, type);

-- ---------------------------------------------------------------------
-- 2. HOTELS + STAFF
-- ---------------------------------------------------------------------
CREATE TABLE hotel (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    slug           VARCHAR(160) NOT NULL UNIQUE,      -- /hotels/grand-plaza-bishkek
    description    TEXT,
    star_rating    SMALLINT CHECK (star_rating BETWEEN 1 AND 5),
    address_line1  VARCHAR(255) NOT NULL,
    address_line2  VARCHAR(255),
    city           VARCHAR(100) NOT NULL,
    region         VARCHAR(100),
    postal_code    VARCHAR(20),
    country_code   CHAR(2)      NOT NULL,             -- ISO 3166-1 alpha-2
    latitude       NUMERIC(9,6),
    longitude      NUMERIC(9,6),
    timezone       VARCHAR(50)  NOT NULL DEFAULT 'UTC',   -- 'Asia/Bishkek'
    currency       CHAR(3)      NOT NULL DEFAULT 'USD',
    contact_email  VARCHAR(255),
    contact_phone  VARCHAR(30),
    check_in_time  TIME         NOT NULL DEFAULT '14:00',
    check_out_time TIME         NOT NULL DEFAULT '12:00',
    avg_rating     NUMERIC(3,2) NOT NULL DEFAULT 0,   -- cached, updated when reviews change
    review_count   INT          NOT NULL DEFAULT 0,
    status         VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
                   CHECK (status IN ('DRAFT','ACTIVE','SUSPENDED','CLOSED')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ
);
CREATE INDEX idx_hotel_city_status ON hotel(city, status) WHERE deleted_at IS NULL;
CREATE INDEX idx_hotel_country     ON hotel(country_code);
CREATE INDEX idx_hotel_name_trgm   ON hotel USING gin (name gin_trgm_ops);

-- Who may manage which hotel, and in what capacity.
CREATE TABLE hotel_staff (
    hotel_id    BIGINT      NOT NULL REFERENCES hotel(id) ON DELETE CASCADE,
    user_id     BIGINT      NOT NULL REFERENCES app_user(id),
    staff_role  VARCHAR(20) NOT NULL
                CHECK (staff_role IN ('OWNER','MANAGER','RECEPTIONIST')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (hotel_id, user_id)
);
CREATE INDEX idx_hotel_staff_user ON hotel_staff(user_id);
-- To forbid one person working for several hotels, add:  UNIQUE (user_id)

-- Staff onboarding: an owner/manager invites a person by e-mail; accepting
-- the invitation creates the hotel_staff row. Nobody self-assigns to a hotel.
CREATE TABLE hotel_invitation (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id    BIGINT       NOT NULL REFERENCES hotel(id) ON DELETE CASCADE,
    email       CITEXT       NOT NULL,
    staff_role  VARCHAR(20)  NOT NULL
                CHECK (staff_role IN ('MANAGER','RECEPTIONIST')),   -- owners are not invited
    token_hash  CHAR(64)     NOT NULL UNIQUE,
    invited_by  BIGINT       NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- only existing staff of THIS hotel can invite
    FOREIGN KEY (hotel_id, invited_by) REFERENCES hotel_staff(hotel_id, user_id)
);
CREATE INDEX idx_hotel_invitation_email ON hotel_invitation(email);

CREATE TABLE amenity (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code      VARCHAR(50)  NOT NULL UNIQUE,           -- 'WIFI', 'POOL'
    name      VARCHAR(100) NOT NULL,
    scope     VARCHAR(10)  NOT NULL CHECK (scope IN ('HOTEL','ROOM')),
    icon      VARCHAR(50)
);

CREATE TABLE hotel_amenity (
    hotel_id   BIGINT NOT NULL REFERENCES hotel(id) ON DELETE CASCADE,
    amenity_id BIGINT NOT NULL REFERENCES amenity(id),
    PRIMARY KEY (hotel_id, amenity_id)
);

-- ---------------------------------------------------------------------
-- 3. ROOM TYPES, ROOMS, STOCK
-- ---------------------------------------------------------------------
-- Category of room ("Deluxe King Sea View"). What guests actually book.
CREATE TABLE room_type (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id      BIGINT       NOT NULL REFERENCES hotel(id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    bed_type      VARCHAR(20)  NOT NULL
                  CHECK (bed_type IN ('SINGLE','DOUBLE','QUEEN','KING','TWIN')),
    bed_count     SMALLINT     NOT NULL DEFAULT 1 CHECK (bed_count > 0),
    max_adults    SMALLINT     NOT NULL CHECK (max_adults > 0),
    max_children  SMALLINT     NOT NULL DEFAULT 0 CHECK (max_children >= 0),
    area_sqm      NUMERIC(6,2),
    base_price    NUMERIC(12,2) NOT NULL CHECK (base_price >= 0),  -- fallback when no rate matches
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (id, hotel_id)                              -- target for composite FKs
);
CREATE INDEX idx_room_type_hotel ON room_type(hotel_id);

CREATE TABLE room_type_amenity (
    room_type_id BIGINT NOT NULL REFERENCES room_type(id) ON DELETE CASCADE,
    amenity_id   BIGINT NOT NULL REFERENCES amenity(id),
    PRIMARY KEY (room_type_id, amenity_id)
);

-- Photos for a hotel; room_type_id set when the photo is of a specific room type.
CREATE TABLE hotel_image (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id     BIGINT       NOT NULL REFERENCES hotel(id) ON DELETE CASCADE,
    room_type_id BIGINT,
    url          VARCHAR(500) NOT NULL,
    caption      VARCHAR(255),
    sort_order   INT          NOT NULL DEFAULT 0,
    is_primary   BOOLEAN      NOT NULL DEFAULT FALSE,
    FOREIGN KEY (room_type_id, hotel_id) REFERENCES room_type(id, hotel_id)
);
CREATE INDEX idx_hotel_image_hotel ON hotel_image(hotel_id, sort_order);
CREATE UNIQUE INDEX uq_hotel_primary_image ON hotel_image(hotel_id) WHERE is_primary AND room_type_id IS NULL;

-- A physical room ("204").
CREATE TABLE room (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id     BIGINT      NOT NULL,
    room_type_id BIGINT      NOT NULL,
    room_number  VARCHAR(10) NOT NULL,
    floor        SMALLINT,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                 CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (id, hotel_id),
    UNIQUE (hotel_id, room_number),
    FOREIGN KEY (room_type_id, hotel_id) REFERENCES room_type(id, hotel_id)
);
CREATE INDEX idx_room_type ON room(room_type_id);

-- Out-of-order periods (maintenance, renovation, owner stay). Rooms in a
-- block must be excluded from availability search.
CREATE TABLE room_block (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_id     BIGINT      NOT NULL,
    hotel_id    BIGINT      NOT NULL,
    start_date  DATE        NOT NULL,
    end_date    DATE        NOT NULL,                 -- exclusive, like check_out
    reason      VARCHAR(30) NOT NULL
                CHECK (reason IN ('MAINTENANCE','RENOVATION','OWNER_USE','OTHER')),
    note        VARCHAR(255),
    created_by  BIGINT REFERENCES app_user(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (end_date > start_date),
    FOREIGN KEY (room_id, hotel_id) REFERENCES room(id, hotel_id) ON DELETE CASCADE,
    FOREIGN KEY (hotel_id, created_by) REFERENCES hotel_staff(hotel_id, user_id),
    EXCLUDE USING gist (room_id WITH =, daterange(start_date, end_date) WITH &&)
);

-- ---------------------------------------------------------------------
-- 4. PRICING + PROMOTIONS
-- ---------------------------------------------------------------------
CREATE TABLE cancellation_policy (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id             BIGINT       NOT NULL REFERENCES hotel(id) ON DELETE CASCADE,
    name                 VARCHAR(100) NOT NULL,       -- 'Free cancellation', 'Non-refundable'
    free_cancel_days     SMALLINT     NOT NULL DEFAULT 0 CHECK (free_cancel_days >= 0),
                                                       -- free until N days before check-in
    penalty_percent      NUMERIC(5,2) NOT NULL DEFAULT 100
                         CHECK (penalty_percent BETWEEN 0 AND 100),
    UNIQUE (id, hotel_id)
);

-- A sellable offer for room types: "Flexible + Breakfast", "Saver (non-refundable)".
CREATE TABLE rate_plan (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id               BIGINT       NOT NULL REFERENCES hotel(id) ON DELETE CASCADE,
    cancellation_policy_id BIGINT       NOT NULL,
    name                   VARCHAR(100) NOT NULL,
    breakfast_included     BOOLEAN      NOT NULL DEFAULT FALSE,
    min_nights             SMALLINT     NOT NULL DEFAULT 1 CHECK (min_nights > 0),
    status                 VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                           CHECK (status IN ('ACTIVE','INACTIVE')),
    UNIQUE (id, hotel_id),
    FOREIGN KEY (cancellation_policy_id, hotel_id) REFERENCES cancellation_policy(id, hotel_id)
);

-- Seasonal / dated price for (room type x rate plan). Non-overlapping.
CREATE TABLE room_rate (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id        BIGINT        NOT NULL,
    room_type_id    BIGINT        NOT NULL,
    rate_plan_id    BIGINT        NOT NULL,
    start_date      DATE          NOT NULL,
    end_date        DATE          NOT NULL,           -- exclusive
    price_per_night NUMERIC(12,2) NOT NULL CHECK (price_per_night >= 0),
    CHECK (end_date > start_date),
    -- room type and rate plan must belong to the same hotel:
    FOREIGN KEY (room_type_id, hotel_id) REFERENCES room_type(id, hotel_id) ON DELETE CASCADE,
    FOREIGN KEY (rate_plan_id, hotel_id) REFERENCES rate_plan(id, hotel_id) ON DELETE CASCADE,
    EXCLUDE USING gist (
        room_type_id WITH =, rate_plan_id WITH =,
        daterange(start_date, end_date) WITH &&
    )
);

CREATE TABLE promo_code (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    hotel_id         BIGINT REFERENCES hotel(id) ON DELETE CASCADE,   -- NULL = platform-wide
    code             VARCHAR(40)  NOT NULL UNIQUE,
    discount_type    VARCHAR(10)  NOT NULL CHECK (discount_type IN ('PERCENT','FIXED')),
    discount_value   NUMERIC(12,2) NOT NULL CHECK (discount_value > 0),
    valid_from       TIMESTAMPTZ  NOT NULL,
    valid_until      TIMESTAMPTZ  NOT NULL,
    max_redemptions  INT,
    redemption_count INT          NOT NULL DEFAULT 0,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    CHECK (valid_until > valid_from)
);

-- ---------------------------------------------------------------------
-- 5. RESERVATIONS + PAYMENTS
-- ---------------------------------------------------------------------
-- The booking header: who, where, money, lifecycle.
CREATE TABLE reservation (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    booking_ref         VARCHAR(12)   NOT NULL UNIQUE,   -- human-friendly: 'HX7K29QP'
    user_id             BIGINT        NOT NULL REFERENCES app_user(id),
    hotel_id            BIGINT        NOT NULL REFERENCES hotel(id),
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','CONFIRMED','CHECKED_IN',
                                          'CHECKED_OUT','CANCELLED','NO_SHOW','EXPIRED')),
    currency            CHAR(3)       NOT NULL,
    subtotal            NUMERIC(12,2) NOT NULL CHECK (subtotal >= 0),
    discount_amount     NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (discount_amount >= 0),
    tax_amount          NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (tax_amount >= 0),
    total_amount        NUMERIC(12,2) NOT NULL CHECK (total_amount >= 0),
    promo_code_id       BIGINT REFERENCES promo_code(id),
    guest_name          VARCHAR(200)  NOT NULL,          -- may differ from account holder
    guest_email         VARCHAR(255)  NOT NULL,
    guest_phone         VARCHAR(30),
    special_requests    TEXT,
    hold_expires_at     TIMESTAMPTZ,                     -- PENDING auto-expires if unpaid
    cancelled_at        TIMESTAMPTZ,
    cancellation_reason VARCHAR(255),
    version             BIGINT        NOT NULL DEFAULT 0, -- JPA @Version, optimistic locking
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    UNIQUE (id, hotel_id)
);
CREATE INDEX idx_reservation_user   ON reservation(user_id, created_at DESC);
CREATE INDEX idx_reservation_hotel  ON reservation(hotel_id, status);
CREATE INDEX idx_reservation_expiry ON reservation(hold_expires_at) WHERE status = 'PENDING';

-- One row per booked room. A reservation can hold several rooms.
-- Rate and cancellation terms are SNAPSHOTTED at booking time.
CREATE TABLE reservation_room (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reservation_id      BIGINT        NOT NULL,
    hotel_id            BIGINT        NOT NULL,
    room_id             BIGINT        NOT NULL,
    rate_plan_id        BIGINT        NOT NULL,
    check_in            DATE          NOT NULL,
    check_out           DATE          NOT NULL,
    adults              SMALLINT      NOT NULL CHECK (adults > 0),
    children            SMALLINT      NOT NULL DEFAULT 0 CHECK (children >= 0),
    price_total         NUMERIC(12,2) NOT NULL CHECK (price_total >= 0),
    cancel_free_until   DATE,                            -- snapshot of policy
    penalty_percent     NUMERIC(5,2)  NOT NULL DEFAULT 0,
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,   -- FALSE once cancelled/expired
    CHECK (check_out > check_in),
    FOREIGN KEY (reservation_id, hotel_id) REFERENCES reservation(id, hotel_id) ON DELETE CASCADE,
    FOREIGN KEY (room_id, hotel_id)        REFERENCES room(id, hotel_id),
    FOREIGN KEY (rate_plan_id, hotel_id)   REFERENCES rate_plan(id, hotel_id),
    -- No double booking, enforced by the database even under concurrent requests:
    EXCLUDE USING gist (
        room_id WITH =,
        daterange(check_in, check_out) WITH &&
    ) WHERE (is_active)
);
CREATE INDEX idx_res_room_reservation ON reservation_room(reservation_id);
CREATE INDEX idx_res_room_dates       ON reservation_room(hotel_id, check_in, check_out) WHERE is_active;

-- Payments and refunds (a refund is a row with kind = 'REFUND').
CREATE TABLE payment (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reservation_id   BIGINT        NOT NULL,
    hotel_id         BIGINT        NOT NULL,              -- lets staff list their hotel's payments
    kind             VARCHAR(10)   NOT NULL DEFAULT 'PAYMENT' CHECK (kind IN ('PAYMENT','REFUND')),
    amount           NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    currency         CHAR(3)       NOT NULL,
    method           VARCHAR(20)   NOT NULL CHECK (method IN ('CARD','BANK_TRANSFER','CASH','WALLET')),
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','AUTHORIZED','CAPTURED','FAILED','CANCELLED')),
    provider         VARCHAR(30),                         -- 'STRIPE', 'SIMULATED'
    provider_ref     VARCHAR(100),                        -- gateway transaction id
    idempotency_key  VARCHAR(64)   NOT NULL UNIQUE,       -- safe client retries
    failure_reason   VARCHAR(255),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    FOREIGN KEY (reservation_id, hotel_id) REFERENCES reservation(id, hotel_id)
);
CREATE INDEX idx_payment_reservation ON payment(reservation_id);
CREATE INDEX idx_payment_hotel       ON payment(hotel_id, created_at DESC);

-- ---------------------------------------------------------------------
-- 6. REVIEWS + FAVORITES
-- ---------------------------------------------------------------------
-- One review per completed stay, so only real guests can review.
CREATE TABLE review (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reservation_id     BIGINT       NOT NULL UNIQUE,
    hotel_id           BIGINT       NOT NULL,   -- denormalized; the FK below forces it to match the stay
    user_id            BIGINT       NOT NULL REFERENCES app_user(id),
    rating             SMALLINT     NOT NULL CHECK (rating BETWEEN 1 AND 5),
    cleanliness_rating SMALLINT CHECK (cleanliness_rating BETWEEN 1 AND 5),
    service_rating     SMALLINT CHECK (service_rating BETWEEN 1 AND 5),
    location_rating    SMALLINT CHECK (location_rating BETWEEN 1 AND 5),
    headline           VARCHAR(150) NOT NULL,
    body               TEXT,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PUBLISHED'
                       CHECK (status IN ('PENDING','PUBLISHED','HIDDEN')),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (id, hotel_id),
    FOREIGN KEY (reservation_id, hotel_id) REFERENCES reservation(id, hotel_id)
);
CREATE INDEX idx_review_hotel ON review(hotel_id, created_at DESC) WHERE status = 'PUBLISHED';

-- Hotel staff reply to a review (one public reply).
CREATE TABLE review_response (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    review_id   BIGINT      NOT NULL UNIQUE,
    hotel_id    BIGINT      NOT NULL,
    staff_id    BIGINT      NOT NULL,
    body        TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    FOREIGN KEY (review_id, hotel_id) REFERENCES review(id, hotel_id) ON DELETE CASCADE,
    -- only staff of the SAME hotel may answer its reviews:
    FOREIGN KEY (hotel_id, staff_id)  REFERENCES hotel_staff(hotel_id, user_id)
);

CREATE TABLE favorite_hotel (
    user_id    BIGINT      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    hotel_id   BIGINT      NOT NULL REFERENCES hotel(id)    ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, hotel_id)
);

-- ---------------------------------------------------------------------
-- 7. AUDIT LOG + TRIGGERS + SEED DATA
-- ---------------------------------------------------------------------
-- Who changed what, when. Written by the service layer (or Hibernate Envers).
CREATE TABLE audit_log (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_id    BIGINT REFERENCES app_user(id) ON DELETE SET NULL,
    hotel_id    BIGINT REFERENCES hotel(id) ON DELETE SET NULL,   -- NULL = platform-level event
    action      VARCHAR(50)  NOT NULL,            -- 'RESERVATION_CANCELLED', 'HOTEL_UPDATED'
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   BIGINT       NOT NULL,
    details     JSONB,                            -- before/after, reason, etc.
    ip_address  INET,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_actor  ON audit_log(actor_id, created_at DESC);
CREATE INDEX idx_audit_hotel  ON audit_log(hotel_id, created_at DESC);

-- Auto-maintain updated_at on every table that has the column.
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['app_user','hotel','room_type','room','reservation','payment','review']
    LOOP
        EXECUTE format(
          'CREATE TRIGGER trg_%I_updated_at BEFORE UPDATE ON %I
           FOR EACH ROW EXECUTE FUNCTION set_updated_at()', t, t);
    END LOOP;
END $$;

INSERT INTO role (code, description) VALUES
  ('GUEST', 'Regular customer who can search and book'),
  ('ADMIN', 'Platform administrator');

INSERT INTO amenity (code, name, scope) VALUES
  ('WIFI','Free Wi-Fi','HOTEL'), ('POOL','Swimming pool','HOTEL'),
  ('PARKING','Parking','HOTEL'), ('SPA','Spa','HOTEL'), ('GYM','Fitness center','HOTEL'),
  ('AC','Air conditioning','ROOM'), ('TV','TV','ROOM'), ('MINIBAR','Mini-bar','ROOM'),
  ('BALCONY','Balcony','ROOM'), ('KETTLE','Kettle','ROOM'), ('SAFE','In-room safe','ROOM');

-- ---------------------------------------------------------------------
-- REFERENCE: availability search (what your repository query will do)
--   Rooms of a hotel that are free for :check_in .. :check_out:
--
--   SELECT r.*
--   FROM room r
--   WHERE r.hotel_id = :hotel AND r.room_type_id = :type AND r.status = 'ACTIVE'
--     AND NOT EXISTS (SELECT 1 FROM reservation_room rr
--                     WHERE rr.room_id = r.id AND rr.is_active
--                       AND daterange(rr.check_in, rr.check_out) && daterange(:check_in, :check_out))
--     AND NOT EXISTS (SELECT 1 FROM room_block b
--                     WHERE b.room_id = r.id
--                       AND daterange(b.start_date, b.end_date) && daterange(:check_in, :check_out));
-- ---------------------------------------------------------------------
