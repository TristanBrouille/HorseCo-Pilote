# Documentation des tests — Back HorseCo-Pilote

Ce document décrit comment le back est testé : la stratégie, comment lancer les tests et ce que couvre chaque classe de test.

**État actuel : 193 tests, tous au vert** (`./mvnw test`).

## Lancer les tests

Toutes les commandes se lancent depuis `horsecopilote/`. Sous Windows, remplacer `./mvnw` par `.\mvnw.cmd`.

```bash
# Prérequis : la base locale doit tourner (utilisée par le test de démarrage)
docker compose up -d

# Tous les tests
./mvnw test

# Une seule classe de test
./mvnw test -Dtest=LessonServiceTest

# Une seule méthode
./mvnw test -Dtest=LessonServiceTest#generateLessonsBooksTheEnrolledMembersWithoutValidation

# Plusieurs classes (motif)
./mvnw test -Dtest='Lesson*Test'
```

Les rapports détaillés (un fichier par classe) sont générés dans `target/surefire-reports/`. Les tests se lancent aussi depuis l'IDE (clic droit sur `src/test/java` → *Run tests*).

## Stratégie

Les tests suivent l'architecture 3 couches de l'application (Controller → Service → Repository). Chaque couche est testée séparément, avec les dépendances des couches voisines simulées.

| Niveau | Outils | Ce qui est vérifié | Classes |
|---|---|---|---|
| **Services** (règles métier) | JUnit 5, Mockito, AssertJ | Chaque règle métier, cas accepté et cas refusé. Les repositories sont simulés (`@Mock`), le **mapper MapStruct est le vrai** (`new <Ressource>MapperImpl(...)`) pour tester aussi les DTO produits | `*ServiceTest`, `HorseEligibilityTest` |
| **Mappers** | JUnit 5, AssertJ | Conversion entité ↔ DTO, champs calculés (heure de fin, compteurs), données masquées selon le rôle | `*MapperTest` |
| **Controllers** (HTTP) | `@WebMvcTest`, MockMvc, Spring Security Test | URL, verbes, codes HTTP, JSON renvoyé, validation des entrées (400), traduction des erreurs (404, 409), **droits par rôle** (401 sans session, 403 mauvais rôle). La vraie `SecurityConfig` est importée, les services sont simulés (`@MockitoBean`) | `*ControllerTest` |
| **Démarrage complet** | `@SpringBootTest` | L'application démarre contre PostgreSQL : migrations Flyway appliquées, entités JPA conformes au schéma (`ddl-auto=validate`), tous les beans câblés | `HorsecopiloteApplicationTests` |

Conventions :

- Noms des tests en anglais, sous forme de phrase décrivant le comportement attendu (`assignHorseRefusesAHorseThatBreaksARule`).
- Structure *given / when / then* : préparation des données, appel, vérifications.
- Les données de test sont construites par des classes utilitaires (`LessonFixtures`, `ReservationFixtures`) plutôt que dupliquées.
- Le rôle de l'utilisateur connecté est simulé avec `@WithMockUser(authorities = "ADMIN" | "COACH" | "MEMBRE")`, et l'absence de session avec `@WithAnonymousUser`.
- Les tests de services fixent la date courante quand une règle en dépend (génération des leçons) pour rester reproductibles.

## Couverture des règles métier

| Règle | Description | Tests |
|---|---|---|
| R1, R5 | Créneau d'1 heure, 12 places au maximum, début à l'heure ou à la demi-heure | `WeeklySlotControllerTest` (`createSlotReturns400WhenCapacityExceedsTwelve`, `createSlotReturns400WhenStartTimeIsNotOnHalfHour`), `WeeklySlotMapperTest` |
| R3, R4, R5 | Inscription à un créneau : pas deux fois, niveau suffisant, créneau non complet, pas de créneau qui se chevauche | `WeeklySlotServiceTest` (`addMember*`, `getEligibleMembers*`, `updateSlotRejects*`) |
| R6, R11 | Le membre ne voit pas les autres participants ; le coach voit ceux de ses leçons, l'admin ceux de toutes | `LessonMapperTest`, `LessonServiceTest` (`…HidesParticipantsOfOtherCoachesLessons`, `…LetsAnAdminSeeAndEditEveryLesson`), `ReservationServiceTest` |
| R7, R7bis | Génération des leçons avec les inscrits ajoutés d'office ; leçons avec de nouveaux inscrits signalées | `LessonServiceTest` (`generateLessons*`, `…LooksForPendingMembersOnlyInOwnLessons`) |
| R9 | Annuler une leçon libère ses chevaux | `LessonServiceTest#updateLessonCancellingReleasesTheHorses` |
| R10 | Demande de modification de cours par le membre, validation par l'admin | `LessonChangeRequestServiceTest`, `LessonChangeRequestControllerTest`, `LessonChangeRequestAdminControllerTest` |
| R12 | Seul le coach de la leçon (ou l'admin) attribue un cheval | `LessonServiceTest` (`…RefusesALessonGivenByAnotherCoach`, `assignHorseLetsAnAdminEditALessonOfAnyCoach`) |
| R13 | Un coach ne donne pas deux cours en même temps | `WeeklySlotServiceTest#createSlotRejectsOverlappingSlotOfSameCoach`, `LessonServiceTest#updateLessonRefusesACoachWhoAlreadyTeachesAtThatTime`, `generateLessonsKeepsExistingLessonsAndSkipsABusyCoach` |
| R14 | Poids du cavalier ≤ 20 % du poids du cheval (limite incluse) | `HorseEligibilityTest` (`aRiderHeavierThanTwentyPercent…`, `aRiderWeighingExactlyTwentyPercent…`) |
| R15 | Galop du cavalier ≥ niveau requis par le cheval | `HorseEligibilityTest` (`aHorseRequiringAHigherLevelIsRefused`, `…ExactlyTheRiderLevelIsAccepted`) |
| R16 | Seul un cheval disponible est attribuable | `HorseEligibilityTest#aHorseThatIsNotAvailableIsRefused` |
| R17 | Un cheval sur une seule leçon à un même horaire | `HorseEligibilityTest` (`…AlreadyRiddenByAnotherParticipant…`, `…AssignedToASimultaneousLessonIsRefused`) |
| R18 | Heures de travail par jour (3 au maximum) | `HorseEligibilityTest#aHorseThatReachedItsDailyHoursIsRefused`, `HorseControllerTest#createHorseReturns400WhenDailyHoursExceedThree` |
| R19 | Pas plus de 2 heures d'affilée | `HorseEligibilityTest` (`aHorseWorkingTheTwoPreviousHoursIsRefused`, `…HourBeforeAndTheHourAfter…`, `aHorseWithABreakBeforeTheLessonIsAccepted`) |
| R20 | Chevaux non sélectionnables listés avec leur motif | `LessonServiceTest#getHorseOptionsListsSelectableHorsesFirstWithTheirReason` |
| Sécurité | Connexion, déconnexion, droits par rôle sur chaque endpoint | `RegistrationLoginControllerTest`, `CustomUserDetailsServiceTest`, tests 401 / 403 de chaque `*ControllerTest` |

## Détail par classe

### Démarrage

| Classe | Tests | Ce qui est vérifié |
|---|---|---|
| `HorsecopiloteApplicationTests` | 1 | L'application démarre avec le profil `dev` contre la base locale (migrations, schéma, beans) |

### Authentification et utilisateurs (`security`, `user`)

| Classe | Tests | Ce qui est vérifié |
|---|---|---|
| `RegistrationLoginControllerTest` | 4 | Connexion réussie (200 + utilisateur), mauvais identifiants (401), déconnexion qui invalide la session, déconnexion sans session |
| `CustomUserDetailsServiceTest` | 1 | L'email de connexion est retrouvé sans tenir compte de la casse ni des espaces |
| `UserServiceTest` | 9 | Recherche d'un coach ou d'un membre par id ou par email, refus si l'utilisateur n'a pas le bon rôle ou n'existe pas, DTO sans mot de passe |
| `CoachControllerTest` | 1 | Liste des coachs |

### Membres (`member`)

| Classe | Tests | Ce qui est vérifié |
|---|---|---|
| `MemberServiceTest` | 2 | Création avec le rôle `MEMBRE` et le mot de passe chiffré, refus d'un email déjà utilisé |
| `MemberControllerTest` | 5 | Création (201 + `Location`), validation des champs (400), email déjà utilisé (409), liste |

### Chevaux (`horse`)

| Classe | Tests | Ce qui est vérifié |
|---|---|---|
| `HorseServiceTest` | 6 | Création (champs nettoyés), modification, cheval introuvable, suppression, refus de supprimer un cheval déjà attribué |
| `HorseControllerTest` | 8 | Création (201), `daily_hours` > 3 et nom vide refusés (400), lieu inconnu (400), liste, suppression (204), cheval attribué (409) |

### Créneaux hebdomadaires (`weeklyslot`)

| Classe | Tests | Ce qui est vérifié |
|---|---|---|
| `WeeklySlotServiceTest` | 20 | Création avec le coach, chevauchement d'un même coach refusé (créneaux qui se suivent acceptés), tri par jour et heure, modification et suppression, ajout et retrait d'inscrits avec toutes les règles (déjà inscrit, complet, niveau, créneau qui se chevauche), capacité ou niveau incompatibles avec les inscrits, membres éligibles |
| `WeeklySlotMapperTest` | 2 | Créneau d'1 heure créé sans coach, heure de fin calculée |
| `WeeklySlotControllerTest` | 22 | CRUD (201, 200, 204, 404), capacité > 12 et heure hors demi-heure (400), coach occupé (409), jour ou id invalides (400), URL inconnue (404), erreur inattendue (500 sans détail technique), gestion des inscrits, droits admin |

### Leçons et attribution des chevaux (`lesson`)

| Classe | Tests | Ce qui est vérifié |
|---|---|---|
| `HorseEligibilityTest` | 14 | Toutes les règles d'attribution d'un cheval (R14 à R19), avec les valeurs limites, et le comptage des heures de travail du jour |
| `LessonServiceTest` | 28 | Agenda de la semaine (coach : ses leçons en avant, participants des autres masqués ; admin : tout est éditable), semaine qui ne commence pas un lundi, sujet et lieu, attribution et retrait d'un cheval, liste des chevaux avec motif, modification admin (coach, lieu, sujet, statut, annulation qui libère les chevaux, coach occupé), génération des leçons (une par créneau et par semaine, inscrits ajoutés d'office, sujet et lieu vides, leçons existantes conservées, coach occupé, date passée ou à plus d'un an refusée) |
| `LessonMapperTest` | 5 | Champs de la leçon, participants visibles seulement si la leçon est éditable, compteur conservé, leçon sans créneau |
| `LessonControllerTest` | 24 | Agenda (coach et admin, membre refusé, 401 sans session, `weekStart` manquant ou invalide), génération (admin seulement, date obligatoire), modification admin (avec sujet et lieu vides), sujet et lieu du coach (obligatoires, leçon d'un autre coach refusée), chevaux (options, attribution, règle non respectée 409, participant inconnu 404, retrait) |

### Cours du membre (`reservation`)

| Classe | Tests | Ce qui est vérifié |
|---|---|---|
| `ReservationServiceTest` | 4 | Leçons de la semaine du membre avec son cheval, semaine qui ne commence pas un lundi, utilisateur qui n'est pas membre |
| `ReservationMapperTest` | 3 | Leçon du membre avec ou sans cheval, leçon d'un autre membre marquée « pas à moi » |
| `ReservationControllerTest` | 7 | Membre pris dans la session, coach et admin refusés (403), sans session (401), `weekStart` manquant ou invalide (400) |

### Demandes de modification de cours (`lessonchange`)

| Classe | Tests | Ce qui est vérifié |
|---|---|---|
| `LessonChangeRequestServiceTest` | 16 | Création (motif nettoyé) et tous les refus : même leçon, membre non inscrit ou déjà inscrit à la cible, leçon passée, cible annulée, autre semaine, complète ou de niveau trop élevé ; acceptation (inscription déplacée, sans son cheval, règles revérifiées), refus, demande déjà traitée ou inconnue |
| `LessonChangeRequestControllerTest` | 6 | Création par le membre connecté (201), motif vide (400), règle non respectée (409), coach refusé (403), liste des demandes, 401 sans session |
| `LessonChangeRequestAdminControllerTest` | 5 | Demandes en attente, acceptation, règle non respectée (409), demande inconnue (404), membre refusé (403) |

## Ajouter un test

1. Placer la classe dans le même package que la classe testée, sous `src/test/java` (ex. `lesson/service/LessonServiceTest`).
2. Service : `@ExtendWith(MockitoExtension.class)`, repositories en `@Mock`, mapper réel en `@Spy`, service en `@InjectMocks`.
3. Controller : `@WebMvcTest(MonController.class)` + `@Import(SecurityConfig.class)`, service en `@MockitoBean`, rôle avec `@WithMockUser`.
4. Tester le cas nominal **et** chaque refus (règle métier, droits, validation).
5. Vérifier que `./mvnw test` reste au vert avant de pousser.
