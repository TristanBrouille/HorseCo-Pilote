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

CREATE TABLE "weekly_slot_assignment"
(
    "id"             SERIAL  NOT NULL,
    "weekly_slot_id" INTEGER NOT NULL,
    "user_id"        INTEGER NOT NULL,
    "horse_id"       INTEGER NULL,
    CONSTRAINT "pk_weekly_slot_assignment" PRIMARY KEY ("id"),
    CONSTRAINT "uc_wsa_slot_member" UNIQUE ("weekly_slot_id", "user_id"),
    CONSTRAINT "uc_wsa_slot_horse" UNIQUE ("weekly_slot_id", "horse_id"),
    CONSTRAINT "fk_wsa_slot" FOREIGN KEY ("weekly_slot_id")
        REFERENCES "weekly_slot" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_wsa_member" FOREIGN KEY ("user_id")
        REFERENCES "member" ("user_id") ON DELETE CASCADE,
    CONSTRAINT "fk_wsa_horse" FOREIGN KEY ("horse_id")
        REFERENCES "horse" ("id") ON DELETE SET NULL
);

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

CREATE TABLE "lesson_participant"
(
    "id"        SERIAL  NOT NULL,
    "lesson_id" INTEGER NOT NULL,
    "user_id"   INTEGER NOT NULL,
    "horse_id"  INTEGER NULL,
    CONSTRAINT "pk_lesson_participant" PRIMARY KEY ("id"),
    CONSTRAINT "uc_lp_lesson_user" UNIQUE ("lesson_id", "user_id"),
    CONSTRAINT "uc_lp_lesson_horse" UNIQUE ("lesson_id", "horse_id"),
    CONSTRAINT "fk_lp_lesson" FOREIGN KEY ("lesson_id")
        REFERENCES "lesson" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_lp_member" FOREIGN KEY ("user_id")
        REFERENCES "member" ("user_id"),
    CONSTRAINT "fk_lp_horse" FOREIGN KEY ("horse_id")
        REFERENCES "horse" ("id"),
);