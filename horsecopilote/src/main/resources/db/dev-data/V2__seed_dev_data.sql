-- ==========================================
-- DONNEES DE TEST (profil dev uniquement)
-- Mot de passe de tous les comptes : "password" (BCrypt)
-- Semaine de reference : lundi 12 octobre 2026
-- ==========================================

-- ---------- Utilisateurs : 1 admin, 3 coachs, 8 membres ----------
INSERT INTO "users" ("id", "role", "last_name", "first_name", "email", "password")
VALUES (1, 'ADMIN', 'Bernard', 'Julie', 'admin@horseco.test', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW'),
       (2, 'COACH', 'Durand', 'Claire', 'claire.durand@horseco.test', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW'),
       (3, 'COACH', 'Lefèvre', 'Marc', 'marc.lefevre@horseco.test', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW'),
       (4, 'COACH', 'Martin', 'Sophie', 'sophie.martin@horseco.test', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW'),
       (5, 'MEMBRE', 'Petit', 'Léa', 'lea.petit@horseco.test', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW'),
       (6, 'MEMBRE', 'Roux', 'Hugo', 'hugo.roux@horseco.test', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW'),
       (7, 'MEMBRE', 'Moreau', 'Chloé', 'chloe.moreau@horseco.test', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW'),
       (8, 'MEMBRE', 'Fournier', 'Tom', 'tom.fournier@horseco.test', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW'),
       (9, 'MEMBRE', 'Girard', 'Emma', 'emma.girard@horseco.test', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW'),
       (10, 'MEMBRE', 'Bonnet', 'Lucas', 'lucas.bonnet@horseco.test', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW'),
       (11, 'MEMBRE', 'Lambert', 'Inès', 'ines.lambert@horseco.test', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW'),
       (12, 'MEMBRE', 'Fontaine', 'Nathan', 'nathan.fontaine@horseco.test', '$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW');

-- ---------- Membres : galops 1 a 7 ----------
INSERT INTO "member" ("user_id", "galop_level", "height_cm", "weight_kg")
VALUES (5, 1, 140, 35.00),
       (6, 2, 150, 45.00),
       (7, 3, 160, 52.00),
       (8, 3, 165, 58.00),
       (9, 4, 168, 60.00),
       (10, 5, 178, 70.00),
       (11, 6, 170, 62.00),
       (12, 7, 182, 75.00);

-- ---------- Chevaux : un de chaque statut, daily_hours <= 3 ----------
INSERT INTO "horse" ("id", "location", "name", "breed", "height_cm", "weight_kg", "daily_hours", "status", "required_level")
VALUES (1, 'BOX', 'Tornado', 'Selle Français', 168, 560, 3, 'DISPONIBLE', 3),
       (2, 'PRE', 'Caramel', 'Connemara', 145, 380, 3, 'DISPONIBLE', 1),
       (3, 'BOX', 'Éclair', 'Pur-sang arabe', 152, 450, 2, 'DISPONIBLE', 4),
       (4, 'PADOCK', 'Noisette', 'Haflinger', 148, 500, 3, 'DISPONIBLE', 1),
       (5, 'BOX', 'Rubis', 'KWPN', 170, 620, 2, 'DISPONIBLE', 5),
       (6, 'BOX', 'Ulysse', 'Anglo-arabe', 163, 540, 3, 'REPOS', 3),
       (7, 'PRE', 'Vanille', 'Welsh', 140, 300, 3, 'PRE', 1),
       (8, 'BOX', 'Zéphyr', 'Hanovrien', 172, 640, 2, 'CONVALESCENCE', 6),
       (9, 'CARRIERE', 'Quartz', 'Lusitanien', 160, 520, 3, 'DISPONIBLE', 2),
       (10, 'PRE', 'Pépite', 'Camargue', 147, 420, 3, 'DISPONIBLE', 1);

-- ---------- Creneaux hebdomadaires (1 h, sans chevauchement par coach) ----------
INSERT INTO "weekly_slot" ("id", "instructor_id", "day_of_week", "start_time", "duration_minutes", "max_capacity", "required_level")
VALUES (1, 2, 'MONDAY', '18:00', 60, 12, 1),
       (2, 2, 'MONDAY', '19:00', 60, 10, 3),
       (3, 3, 'WEDNESDAY', '14:00', 60, 8, 1),
       (4, 4, 'WEDNESDAY', '17:00', 60, 8, 4),
       (5, 3, 'FRIDAY', '18:00', 60, 6, 5),
       (6, 4, 'SATURDAY', '10:00', 60, 12, 2),
       (7, 4, 'SATURDAY', '11:00', 60, 6, 6);

-- ---------- Inscriptions annuelles (galop >= niveau requis) ----------
INSERT INTO "weekly_slot_member" ("weekly_slot_id", "user_id")
VALUES (1, 5), (1, 6), (1, 7),
       (2, 7), (2, 8), (2, 9),
       (3, 5), (3, 6),
       (4, 9), (4, 10), (4, 11),
       (5, 10), (5, 11), (5, 12),
       (6, 6), (6, 8),
       (7, 11), (7, 12);

-- ---------- Lecons de la semaine du 12/10/2026 ----------
-- Creneaux 6 et 7 (samedi) : lecons pas encore creees par le coach
INSERT INTO "lesson" ("id", "weekly_slot_id", "instructor_id", "location", "start_datetime", "topic", "status")
VALUES (1, 1, 2, 'CARRIERE', '2026-10-12 18:00', 'CSO', 'TERMINEE'),
       (2, 2, 2, 'MANEGE', '2026-10-12 19:00', 'DRESS', 'TERMINEE'),
       (3, 3, 3, 'MANEGE', '2026-10-14 14:00', 'MISE_EN_SELLE', 'PREVUE'),
       (4, 4, 4, 'CARRIERE', '2026-10-14 17:00', 'CSO', 'PREVUE'),
       (5, 5, 3, 'CARRIERE', '2026-10-16 18:00', 'TAP', 'ANNULEE');

-- ---------- Participants valides et chevaux attribues ----------
-- Lecon 3 : Hugo valide, cheval pas encore attribue
-- Lecon 4 : Ines inscrite mais pas encore validee (inscription tardive, R7bis)
-- Lecon 5 : annulee, chevaux liberes (R9)
INSERT INTO "lesson_booking" ("id", "lesson_id", "user_id", "horse_id")
VALUES (1, 1, 5, 2),
       (2, 1, 6, 4),
       (3, 1, 7, 9),
       (4, 2, 7, 1),
       (5, 2, 8, 9),
       (6, 2, 9, 3),
       (7, 3, 5, 10),
       (8, 3, 6, NULL),
       (9, 4, 9, 1),
       (10, 4, 10, 5);

-- ---------- Sequences alignees sur les ids inseres ----------
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX("id") FROM "users"));
SELECT setval(pg_get_serial_sequence('horse', 'id'), (SELECT MAX("id") FROM "horse"));
SELECT setval(pg_get_serial_sequence('weekly_slot', 'id'), (SELECT MAX("id") FROM "weekly_slot"));
SELECT setval(pg_get_serial_sequence('lesson', 'id'), (SELECT MAX("id") FROM "lesson"));
SELECT setval(pg_get_serial_sequence('lesson_booking', 'id'), (SELECT MAX("id") FROM "lesson_booking"));
