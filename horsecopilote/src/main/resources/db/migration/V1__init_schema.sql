-- ==========================================
-- 1. REFERENCE TABLES AND USERS
-- ==========================================

CREATE TABLE "users"
(
    "id"         SERIAL       NOT NULL,
    "role"       VARCHAR(25)  NOT NULL,
    "last_name"  VARCHAR(100) NOT NULL,
    "first_name" VARCHAR(100) NOT NULL,
    "email"      VARCHAR(255) NOT NULL,
    "password"   VARCHAR(255) NOT NULL,
    CONSTRAINT "pk_users" PRIMARY KEY ("id"),
    CONSTRAINT "uc_users_email" UNIQUE ("email")
);

CREATE TABLE "member"
(
    "user_id"     INTEGER       NOT NULL,
    "galop_level" INTEGER       NOT NULL,
    "height_cm"   INTEGER       NOT NULL,
    "weight_kg"   NUMERIC(5, 2) NOT NULL,
    CONSTRAINT "pk_member" PRIMARY KEY ("user_id"),
    CONSTRAINT "fk_member_user_id" FOREIGN KEY ("user_id")
        REFERENCES "users" ("id") ON DELETE CASCADE
);

-- ==========================================
-- 2. HORSES AND LOCATIONS
-- ==========================================

CREATE TABLE "location"
(
    "id"   SERIAL       NOT NULL,
    "name" VARCHAR(100) NOT NULL,
    CONSTRAINT "pk_location" PRIMARY KEY ("id"),
    CONSTRAINT "uc_location_name" UNIQUE ("name")
);

CREATE TABLE "horse"
(
    "id"             SERIAL       NOT NULL,
    "location_id"    INTEGER      NOT NULL,
    "name"           VARCHAR(100) NOT NULL,
    "breed"          VARCHAR(100) NOT NULL,
    "height_cm"      INTEGER      NOT NULL,
    "daily_hours"    INTEGER      NOT NULL,
    "status"         VARCHAR(50)  NOT NULL,
    "required_level" INTEGER      NOT NULL,
    CONSTRAINT "pk_horse" PRIMARY KEY ("id"),
    CONSTRAINT "fk_horse_location_id" FOREIGN KEY ("location_id")
        REFERENCES "location" ("id")
);

-- ==========================================
-- 3. YEARLY PLANNING (STRUCTURE STATIQUE)
-- ==========================================

CREATE TABLE "weekly_slot"
(
    "id"               SERIAL      NOT NULL,
    "instructor_id"    INTEGER     NOT NULL,
    "day_of_week"      VARCHAR(15) NOT NULL,
    "start_time"       TIME        NOT NULL,
    "duration_minutes" INTEGER     NOT NULL,
    "max_capacity"     INTEGER     NOT NULL,
    "required_level"   INTEGER     NOT NULL,
    CONSTRAINT "pk_weekly_slot" PRIMARY KEY ("id"),
    CONSTRAINT "fk_weekly_slot_instructor_id" FOREIGN KEY ("instructor_id")
        REFERENCES "users" ("id")
);

-- Inscriptions annuelles des membres au créneau (sans cheval)
CREATE TABLE "weekly_slot_member"
(
    "weekly_slot_id" INTEGER NOT NULL,
    "user_id"        INTEGER NOT NULL,
    CONSTRAINT "pk_weekly_slot_member" PRIMARY KEY ("weekly_slot_id", "user_id"),
    CONSTRAINT "fk_wsm_slot" FOREIGN KEY ("weekly_slot_id")
        REFERENCES "weekly_slot" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_wsm_member" FOREIGN KEY ("user_id")
        REFERENCES "member" ("user_id") ON DELETE CASCADE
);

-- ==========================================
-- 4. REALIZED SESSIONS & ATTRIBUTION DU CHEVAL
-- ==========================================

CREATE TABLE "lesson"
(
    "id"             SERIAL       NOT NULL,
    "weekly_slot_id" INTEGER NULL,
    "instructor_id"  INTEGER      NOT NULL,
    "location_id"    INTEGER      NOT NULL,
    "start_datetime" TIMESTAMP    NOT NULL,
    "topic"          VARCHAR(150) NOT NULL,
    "status"         VARCHAR(50)  NOT NULL,
    CONSTRAINT "pk_lesson" PRIMARY KEY ("id"),
    CONSTRAINT "fk_lesson_weekly_slot_id" FOREIGN KEY ("weekly_slot_id")
        REFERENCES "weekly_slot" ("id") ON DELETE SET NULL,
    CONSTRAINT "fk_lesson_instructor_id" FOREIGN KEY ("instructor_id")
        REFERENCES "users" ("id"),
    CONSTRAINT "fk_lesson_location_id" FOREIGN KEY ("location_id")
        REFERENCES "location" ("id")
);

-- C'est ici qu'on attribue le cheval au cavalier pour la séance !
CREATE TABLE "lesson_booking"
(
    "id"        SERIAL  NOT NULL,
    "lesson_id" INTEGER NOT NULL,
    "user_id"   INTEGER NOT NULL,
    "horse_id"  INTEGER NULL, -- NULL tant que le moniteur n'a pas assigné le cheval
    CONSTRAINT "pk_lesson_booking" PRIMARY KEY ("id"),
    -- Un cavalier ne participe qu'une fois à la même séance :
    CONSTRAINT "uc_lb_lesson_user" UNIQUE ("lesson_id", "user_id"),
    -- Un cheval ne peut pas être monté par 2 personnes sur la même séance :
    CONSTRAINT "uc_lb_lesson_horse" UNIQUE ("lesson_id", "horse_id"),
    CONSTRAINT "fk_lb_lesson" FOREIGN KEY ("lesson_id")
        REFERENCES "lesson" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_lb_member" FOREIGN KEY ("user_id")
        REFERENCES "member" ("user_id") ON DELETE CASCADE,
    CONSTRAINT "fk_lb_horse" FOREIGN KEY ("horse_id")
        REFERENCES "horse" ("id") ON DELETE SET NULL
);