-- ==========================================
-- 1. REFERENCE TABLES AND USERS
-- ==========================================

CREATE TABLE "users" (
    "id" SERIAL   NOT NULL,
    "role" VARCHAR(25)   NOT NULL,
    "last_name" VARCHAR(100)   NOT NULL,
    "first_name" VARCHAR(100)   NOT NULL,
    "email" VARCHAR(255)   NOT NULL,
    "password" VARCHAR(255)   NOT NULL,
    CONSTRAINT "pk_users" PRIMARY KEY (
        "id"
     ),
    CONSTRAINT "uc_users_email" UNIQUE (
        "email"
    )
);

CREATE TABLE "member" (
    -- The primary key is also a foreign key to users (1-to-1 relationship)
    "user_id" INTEGER   NOT NULL,
    "galop_level" INTEGER   NOT NULL,
    "height_cm" INTEGER   NOT NULL,
    "weight_kg" NUMERIC(5,2)   NOT NULL,
    CONSTRAINT "pk_member" PRIMARY KEY (
        "user_id"
     )
);

-- ==========================================
-- 2. HORSES AND LOCATIONS
-- ==========================================
CREATE TABLE "horse" (
    "id" SERIAL   NOT NULL,
    "name" VARCHAR(100)   NOT NULL,
    "breed" VARCHAR(100)   NOT NULL,
    "height_cm" INTEGER   NOT NULL,
    "daily_hours" Integer   NOT NULL,
    "status" VARCHAR(50)   NOT NULL,
    "required_level" INTEGER   NOT NULL,
    CONSTRAINT "pk_horse" PRIMARY KEY (
        "id"
     )
);

CREATE TABLE "location" (
    "id" SERIAL   NOT NULL,
    "name" VARCHAR(100)   NOT NULL,
    CONSTRAINT "pk_location" PRIMARY KEY (
        "id"
     ),
    CONSTRAINT "uc_location_name" UNIQUE (
        "name"
    )
);

-- ==========================================
-- 3. YEARLY STRUCTURE
-- ==========================================
CREATE TABLE "weekly_slot" (
    "id" SERIAL   NOT NULL,
    "day_of_week" VARCHAR(15)   NOT NULL,
    "start_time" TIME   NOT NULL,
    "duration_minutes" INTEGER   NOT NULL,
    "max_capacity" INTEGER   NOT NULL,
    "required_level" INTEGER   NOT NULL,
    "user_id" INTEGER[]   NOT NULL,
    "instructor_id" INTEGER   NOT NULL,
    CONSTRAINT "pk_weekly_slot" PRIMARY KEY (
        "id"
     )
);

-- ==========================================
-- 4. ACTUAL SESSIONS
-- ==========================================
CREATE TABLE "lesson" (
    "id" SERIAL   NOT NULL,
    "weekly_slot_id" INTEGER   NULL,
    "instructor_id" INTEGER   NOT NULL,
    "location_id" INTEGER   NOT NULL,
    "start_datetime" TIMESTAMP   NOT NULL,
    "topic" VARCHAR(150)   NOT NULL,
    "status" VARCHAR(50)   NOT NULL,
    CONSTRAINT "pk_lesson" PRIMARY KEY (
        "id"
     )
);

ALTER TABLE "member" ADD CONSTRAINT "fk_member_user_id" FOREIGN KEY("user_id")
REFERENCES "users" ("id");

ALTER TABLE "weekly_slot" ADD CONSTRAINT "fk_weekly_slot_instructor_id" FOREIGN KEY("instructor_id")
REFERENCES "users" ("id");

ALTER TABLE "lesson" ADD CONSTRAINT "fk_lesson_weekly_slot_id" FOREIGN KEY("weekly_slot_id")
REFERENCES "weekly_slot" ("id");

ALTER TABLE "lesson" ADD CONSTRAINT "fk_lesson_instructor_id" FOREIGN KEY("instructor_id")
REFERENCES "users" ("id");

ALTER TABLE "lesson" ADD CONSTRAINT "fk_lesson_location_id" FOREIGN KEY("location_id")
REFERENCES "location" ("id");
