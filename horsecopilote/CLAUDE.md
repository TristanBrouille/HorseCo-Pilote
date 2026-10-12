# HorseCo-Pilote — Contexte projet

CRM de gestion d'un centre équestre : chevaux, cours, membres, coachs.
Source unique : ce fichier vit dans le back ; le `CLAUDE.md` du front ne fait que l'importer (`@chemin`).

## 1. Dépôts

| Partie | Chemin local | GitHub | Rôle |
|---|---|---|---|
| Back | `C:\Users\Basti\Programmation\HorseCo-Pilote\horsecopilote` | `TristanBrouille/HorseCo-Pilote` (racine git = `HorseCo-Pilote`) | API REST Spring Boot |
| Front | `C:\Users\Basti\Programmation\HorseCo-Pilote-Front\horsecopilote-front` | `TristanBrouille/HorseCo-Pilote-Front` (public, racine git = `HorseCo-Pilote-Front`) | Application Angular |

Le front consomme uniquement l'API REST du back. Aucune règle métier n'est garantie par le seul front : le front **affiche** (grise, masque), le back **refuse**.

## 2. Stack et commandes

### Back
- Java 25, Spring Boot 4.1.1, Maven (wrapper `./mvnw`)
- Spring Web MVC, Spring Data JPA, Spring Security, Lombok
- PostgreSQL 17 via `compose.yaml` (port hôte **5433**, base/user/mdp `horsecopilote` / `horse` / `horse`)
- Flyway (`src/main/resources/db/migration`), `ddl-auto=validate`, `open-in-view=false`
- Package racine : `com.epfjava.horsecopilote`
- `spring-boot-starter-validation` **absent** du `pom.xml` : à ajouter dès le premier DTO validé.

Commandes : `docker compose up -d` · `./mvnw spring-boot:run` · `./mvnw test`

### Front
- Angular 22 (composants standalone, signals), TypeScript 6, RxJS 7.8
- Tests : Vitest (`@angular/build:unit-test`) + jsdom
- Prettier : `printWidth` 100, `singleQuote`
- État actuel : projet vierge (`routes` vide, aucun service HTTP)

Commandes : `npm start` (ng serve) · `npm run build` · `npm test`

## 3. État actuel du code

- Back : seulement `HorsecopiloteApplication` et la migration `V1__init_schema.sql`. Aucune entité, aucun repository, service ou controller.
- Front : squelette généré par Angular CLI.
- Dépôts git à la racine des dossiers parents (`HorseCo-Pilote`, `HorseCo-Pilote-Front`).

## 4. Rôles

Trois rôles uniquement (colonne `users.role`, enum `Role`) :

| Rôle | Valeur | Description |
|---|---|---|
| Membre | `MEMBER` | Cavalier client du centre. Possède une ligne dans `member` (niveau de galop, taille, poids). |
| Coach | `COACH` | Moniteur : crée chaque semaine les leçons de ses créneaux et attribue les chevaux. |
| Admin | `ADMIN` | Gère tout le centre (membres, coachs, chevaux, créneaux) et valide les modifications de cours. |

## 5. Fonctionnement des cours

1. **L'admin crée les créneaux hebdomadaires** (`weekly_slot`) : un horaire fixe d'**1 heure** dans la semaine (jour + heure de début), avec un coach, un niveau requis et une capacité maximale de 12. Il peut les modifier.
2. **Le membre s'inscrit à un créneau** (`weekly_slot_member`), pas à une leçon. L'inscription est **valable un an**. Pour l'instant, **pas de désinscription**.
3. **Chaque semaine, le coach crée la leçon** (`lesson`) de chacun de ses créneaux, avec son type (`topic`) et son lieu.
4. **Le coach valide les membres** de la leçon : ce n'est pas automatique. Il part des inscrits du créneau et crée un `lesson_booking` par membre validé.
5. **Le coach attribue un cheval** à chaque membre validé (`lesson_booking.horse_id`).
6. **Inscription tardive** : si un membre s'inscrit à un créneau alors que la leçon de la semaine est déjà créée, la leçon est **affichée en surbrillance dans l'agenda** du coach, pour qu'il valide ce membre et lui attribue un cheval.
7. **Fin d'inscription** : au bout d'un an, le membre est **désinscrit automatiquement** du créneau (tâche planifiée côté back).

## 6. Énumérations

Les valeurs des enums sont en français, telles que définies par l'équipe. Elles sont stockées en `VARCHAR` (`@Enumerated(EnumType.STRING)`).

| Enum | Valeurs | Utilisation |
|---|---|---|
| `Role` | `MEMBER`, `COACH`, `ADMIN` | `users.role` |
| `Location` | `CARRIERE`, `PRE`, `MANEGE`, `BOX`, `PADOCK` | Remplace la table `location` : lieu actuel du cheval et lieu de la leçon |
| `HorseStatus` | `REPOS`, `PRE`, `DISPONIBLE`, `CONVALESCENCE` | `horse.status` — seul `DISPONIBLE` permet d'affecter le cheval à une leçon |
| `LessonType` | `CSO`, `DRESS`, `TAP`, `ETHOLOGIE`, `MISE_EN_SELLE` | `lesson.topic` — discipline affichée dans le descriptif de la leçon (libellé « MISE EN SELLE ») |
| `LessonStatus` | `PREVUE`, `ANNULEE`, `TERMINEE` | `lesson.status` — `ANNULEE` libère les chevaux (R9) |
| `DayOfWeek` | `java.time.DayOfWeek` | `weekly_slot.day_of_week` |

## 7. Modèle de données

### Schéma V1 actuel

```
users (id, role, last_name, first_name, email UNIQUE, password)
 └─ member (user_id PK/FK → users, galop_level, height_cm, weight_kg)

location (id, name UNIQUE)                       -- à supprimer (devient l'enum Location)
horse (id, location_id → location, name, breed, height_cm,
       daily_hours, status, required_level)

weekly_slot (id, instructor_id → users, day_of_week, start_time,
             duration_minutes, max_capacity, required_level)   -- créneau hebdo d'1 h
weekly_slot_member (weekly_slot_id, user_id → member)          -- inscription annuelle

lesson (id, weekly_slot_id → weekly_slot NULL, instructor_id → users,
        location_id → location, start_datetime,
        topic,   -- LessonType
        status)  -- LessonStatus (annulée ou non)            -- leçon d'une semaine donnée
lesson_booking (id, lesson_id, user_id → member, horse_id → horse NULL)
   UNIQUE (lesson_id, user_id)   -- un membre une seule fois par leçon
   UNIQUE (lesson_id, horse_id)  -- un cheval une seule fois par leçon
```

- `horse_id` est NULL tant que le coach n'a pas attribué de cheval.
- Un coach n'a pas de ligne `member`, donc il ne peut pas être inscrit : la règle est garantie par les clés étrangères.
- **Pas de gestion des box en tant que table** : `BOX` est seulement une valeur de l'enum `Location`.
- Niveaux = galops (entiers). Un membre est « compatible » avec un niveau requis si `galop_level >= required_level`.

### Changements de schéma à faire (migration V2)

Ne jamais modifier `V1__init_schema.sql` : toute évolution passe par une nouvelle migration `V2__…`.

1. **Supprimer la table `location`** : remplacer `horse.location_id` et `lesson.location_id` par une colonne `location VARCHAR` (enum `Location`).
2. **Ajouter `horse.weight_kg`** : nécessaire pour la règle des 20 % du poids (R14).
3. **Ajouter une date d'inscription** sur `weekly_slot_member` (ex. `enrolled_at`) pour la désinscription automatique au bout d'un an.
4. **`lesson.weekly_slot_id` devient obligatoire** (`NOT NULL`) : toute leçon vient d'un créneau.

Les limites (12 participants, `daily_hours` ≤ 3, 2 h consécutives, 20 % du poids…) sont **contrôlées dans le back (services / validation des entités), pas par des `CHECK` en base**. Elles seront écrites avec les entités.

## 8. Règles métier

Légende : **[DB]** garanti par la base · **[S]** vérifié dans le back (renvoie une erreur) · **[UI]** comportement d'affichage du front.

### Créneaux et inscriptions
- R1. L'admin crée et modifie les créneaux hebdomadaires ; un créneau dure 1 heure. [S]
- R2. Un membre s'inscrit lui-même à un créneau ; l'inscription est valable un an, puis il est **désinscrit automatiquement**. Pas de désinscription manuelle pour l'instant. [S]
- R3. Un membre ne peut pas être inscrit deux fois au même créneau ni à la même leçon. [DB]
- R4. Un membre voit tous les créneaux ; ceux qui ne correspondent pas à son niveau sont **affichés grisés** et non réservables. [UI] + refus côté back [S]
- R5. Un créneau (et donc une leçon) a au maximum **12 participants** (1 membre = 1 cheval). [S]
- R6. Un membre consulte ses cours mais **ne voit pas les autres inscrits**. [S] (le DTO « membre » n'expose pas les participants)

### Leçons
- R7. Chaque semaine, le coach crée les leçons de ses créneaux, puis **valide lui-même** les membres de chaque leçon parmi les inscrits du créneau (pas d'ajout automatique). [S]
- R7bis. Un membre qui s'inscrit à un créneau dont la leçon de la semaine existe déjà met la leçon **en surbrillance dans l'agenda** du coach (le back renvoie un indicateur sur la leçon, ex. `hasPendingMembers`). [S] + [UI]
- R8. Le type de leçon (`lesson.topic` : `CSO`, `DRESS`, `TAP`, `ETHOLOGIE`, `MISE EN SELLE`) est affiché dans le descriptif. [UI]
- R9. Un cours annulé (`lesson.status`) libère automatiquement ses chevaux. [S]
- R10. Une modification de cours doit être validée par l'admin. [S] — **à implémenter en dernier**.

### Coachs
- R11. Un coach consulte ses cours **et voit leurs participants**.
- R12. Un coach attribue un cheval à chaque participant de ses leçons. [S]
- R13. Un coach ne peut pas donner deux cours en même temps (créneaux qui se chevauchent interdits pour un même coach). [S]
- Un coach ne peut pas s'inscrire comme participant. [DB] (pas de ligne `member`)

### Chevaux (vérifiés à l'attribution)
- R14. **Poids** : le poids du cavalier ne doit pas dépasser **20 % du poids du cheval** (`member.weight_kg <= 0.20 × horse.weight_kg`). [S]
- R15. **Niveau** : `member.galop_level >= horse.required_level`. [S]
- R16. **Statut** : seul un cheval `DISPONIBLE` peut être affecté à une leçon. [S]
- R17. **Une leçon à la fois** : un cheval n'est affecté qu'à une seule leçon sur un même horaire. [DB] dans une même leçon, [S] entre leçons simultanées
- R18. **Heures par jour** : `daily_hours` = nombre maximal d'heures de travail du cheval dans la journée, **3 au maximum**. Le nombre de leçons du cheval dans la journée (1 h chacune) ne doit pas dépasser `daily_hours`. [S]
- R19. **Heures consécutives** : un cheval ne peut pas travailler plus de **2 heures d'affilée**. S'il travaille sur deux créneaux qui se suivent, il ne peut pas être pris pour le créneau suivant ; il lui faut une pause, quelle que soit sa durée. [S]
- R20. **Affichage** : dans l'écran d'attribution, un cheval non sélectionnable (R14 à R19) est **affiché grisé**, avec le motif. [UI] Le back expose un endpoint qui renvoie les chevaux avec un indicateur `selectable` et un motif.

### Admin
- R21. L'admin voit et modifie toutes les données nécessaires à la gestion du centre : membres, coachs, chevaux, créneaux, leçons.
- R22. L'admin crée les nouveaux membres et les coachs.

## 9. Matrice des droits

| Action | MEMBER | COACH | ADMIN |
|---|---|---|---|
| Voir les créneaux / leçons | ✅ tous (hors niveau = grisés) | ✅ les siens | ✅ tous |
| Voir les participants | ❌ | ✅ ses cours | ✅ |
| S'inscrire à un créneau | ✅ lui-même | ❌ | ✅ (pour un membre) |
| Créer / modifier les créneaux | ❌ | ❌ | ✅ |
| Créer les leçons de la semaine | ❌ | ✅ ses créneaux | ✅ |
| Attribuer un cheval | ❌ | ✅ ses leçons | ✅ |
| CRUD chevaux | ❌ | ❌ (lecture) | ✅ |
| CRUD membres / coachs | ❌ | ❌ | ✅ |
| Modifier / annuler une leçon | ❌ | demande de modification (R10) | ✅ + validation |

## 10. Fonctionnalités (backlog par ordre de réalisation)

1. **Socle** : enums (§6), migration V2 (§7), `spring-boot-starter-validation`, `@RestControllerAdvice` global, CORS pour le front.
2. **Authentification** : login email + mot de passe (BCrypt), sécurisation des endpoints par rôle ; côté front : page de login, intercepteur HTTP, guards par rôle.
3. **Gestion des utilisateurs (admin)** : CRUD membres (niveau de galop, taille, poids) et coachs.
4. **Gestion des chevaux (admin)** : CRUD, statut, lieu, poids, `daily_hours` ≤ 3.
5. **Créneaux hebdomadaires (admin)** : CRUD des `weekly_slot` (1 h, coach, niveau, capacité ≤ 12), contrôle anti-chevauchement des coachs (R13).
6. **Inscriptions (membre)** : liste des créneaux avec hors-niveau grisés (R4), inscription annuelle (R2, R3, R5), « mes cours » sans les autres participants (R6), désinscription automatique après un an (tâche planifiée).
7. **Leçons hebdomadaires (coach)** : création des leçons de la semaine à partir des créneaux, avec type de leçon et lieu, validation des membres (R7, R8), surbrillance des leçons avec inscription tardive (R7bis).
8. **Attribution des chevaux (coach)** : vue de ses leçons avec participants (R11), liste des chevaux avec `selectable` + motif (R14 à R20), attribution (R12).
9. **Annulation d'une leçon** : libère les chevaux (R9).
10. **Validation admin des modifications de cours** (R10) : en dernier.

### Issues GitHub existantes

| Dépôt | Issue | État | Lien avec le backlog |
|---|---|---|---|
| Front | #1 « ETQ Utilisateur je veux avoir une page d'authentification » (assignée à TristanBrouille) | ouverte | Fonctionnalité 2 (Authentification) |
| Back | non consultées (dépôt privé) | — | — |

Format des issues : user stories « ETQ <rôle> je veux … ».

## 11. Conventions

### Back (détail dans `.claude/skills/spring-feature-developer`)
- Architecture 3 couches stricte : Controller → Service → Repository. Un controller n'appelle jamais un repository, un service ne manipule jamais de DTO.
- Identifiants, tables, colonnes, messages et tests **en anglais**. Exception : les valeurs des enums métier (§6) sont en français.
- DTO = `record` (un par fichier, request/response séparés), un `@Component` mapper par ressource.
- Injection par constructeur, champs `final`. Lombok autorisé (déjà présent).
- Erreurs : `ResourceNotFoundException` → 404, validation → 400, violation de règle métier → 409/422 en `ProblemDetail`.
- Limites métier dans le back (validation + services), pas de `CHECK` en base.
- Chaque changement de schéma = nouvelle migration Flyway ; ne jamais modifier une migration existante ; demander avant de modifier une entité existante.
- Tests : services (JUnit 5 + Mockito), mappers, controllers (`@WebMvcTest`). `./mvnw test` doit rester vert.

### Front
- **Direction artistique** : suivre la skill `.claude/skills/horseco-design-system` du dépôt front (tokens couleurs/typo, composants `hc-*`, états grisé/surbrillance), identique à la maquette Figma « HorseCo-Pilote — Maquette ».
- Composants standalone et signals, un dossier par fonctionnalité (`features/horses`, `features/slots`, `features/lessons`…), services HTTP dans `core/`.
- Modèles TypeScript alignés sur les DTO de réponse du back ; enums TypeScript identiques aux enums Java.
- Les règles R4, R20 (éléments grisés) et R7bis (surbrillance) s'appuient sur les indicateurs renvoyés par l'API, sans recalculer les règles côté front.
- Formatage Prettier du projet, tests Vitest.

### Git (back + front)
- Les deux dépôts se commitent **toujours ensemble** : à chaque demande de commit, vérifier `git status` dans les deux, et commiter chacun s'il a des changements.
- Même nom de branche et même message de commit dans les deux dépôts, pour pouvoir les relier.
- Ne jamais commiter sur `main` : créer la branche dans les deux dépôts d'abord.
- Ne pas pousser sans demande explicite ; quand c'est demandé, pousser les deux.

## 12. Points encore ouverts

- Issues du dépôt back `TristanBrouille/HorseCo-Pilote` (privé) à intégrer au backlog.
