# HorseCo-Pilote — Back (API REST)

CRM de gestion d'un centre équestre : chevaux, créneaux, leçons, membres et coachs.

Ce dépôt contient l'**API REST Spring Boot**. L'interface web Angular est dans le dépôt [HorseCo-Pilote-Front](https://github.com/TristanBrouille/HorseCo-Pilote-Front) : il faut lancer les deux pour utiliser l'application.

## Sommaire

- [Utilisation de l'application](#utilisation-de-lapplication)
- [Lancer l'application](#lancer-lapplication)
- [Comptes de test](#comptes-de-test)
- [Documentation de l'API (Swagger)](#documentation-de-lapi-swagger)
- [Tests](#tests)
- [Notre utilisation de l'IA](#notre-utilisation-de-lia)

## Utilisation de l'application

HorseCo-Pilote est un CRM de gestion d'un centre équestre. Il réunit trois profils :

| Rôle | Qui ? | Ce qu'il fait dans l'application |
|---|---|---|
| **Membre** | Cavalier client du centre (niveau de galop, taille, poids) | Consulte ses cours et le cheval qui lui est attribué, demande à changer de cours |
| **Coach** | Moniteur | Prépare ses leçons : sujet, lieu, cheval de chaque cavalier |
| **Admin** | Gérant du centre | Gère les membres, les chevaux, les créneaux et toutes les leçons, valide les demandes de changement |

### Comment fonctionnent les cours

1. **L'admin crée les créneaux hebdomadaires** : un horaire fixe d'**1 heure** répété chaque semaine (ex. « lundi 18 h – 19 h »), avec un coach, un niveau de galop minimum et **12 places au maximum**.
2. **Les membres sont inscrits à un créneau pour l'année** (et non à une leçon isolée).
3. **L'admin génère les leçons** de tous les créneaux jusqu'à une date choisie : une leçon par créneau et par semaine, avec les membres inscrits déjà ajoutés comme participants.
4. **Le coach prépare chacune de ses leçons** : il choisit le sujet (CSO, DRESS, TAP, ÉTHOLOGIE, MISE EN SELLE) et le lieu (carrière, manège, pré, box, padock), puis attribue un cheval à chaque cavalier.
5. **Le membre consulte ses cours** et le cheval qui lui est attribué. Il peut demander à passer dans un autre cours de la même semaine ; l'admin accepte ou refuse.

Toutes les règles sont vérifiées par le back : le front grise ou masque ce qui n'est pas possible, mais c'est le serveur qui refuse.

### Fonctionnalités communes

- **Connexion** par email et mot de passe (session sécurisée, mot de passe chiffré en BCrypt) et **déconnexion** depuis l'en-tête.
- **Tableau de bord** d'accueil : une carte par fonctionnalité du rôle connecté. Les fonctionnalités pas encore développées sont affichées grisées avec la mention « Bientôt disponible ».
- **Menu latéral** propre à chaque rôle. Chaque page est protégée : un membre ne peut pas ouvrir une page admin, même en tapant l'URL.

### Membre

**Mes cours** (`/membre/cours`)
- Calendrier de la semaine (jours × heures) avec **tous les cours du centre** : les siens sont mis en avant, les autres affichés en retrait, les cours annulés signalés.
- Navigation semaine précédente / suivante et retour à « Cette semaine ».
- Détail d'un cours : date, horaire, lieu, coach, statut (prévu, annulé, terminé) et **cheval attribué** (ou « pas encore attribué »).
- Un membre **ne voit jamais la liste des autres participants**.
- **Demander une modification** d'un cours à venir : choix d'un autre cours à venir de la même semaine et motif (500 caractères au maximum). Le cours cible doit avoir de la place et correspondre au niveau du membre.
- **Mes demandes de modification** : historique des demandes avec leur statut (en attente, acceptée, refusée).

### Coach

**Agenda de la semaine** (`/coach/cours`)
- Calendrier de **toutes les leçons du centre** : les siennes en avant, celles des autres coachs en retrait, les leçons annulées signalées.
- **Surbrillance** des leçons dont un membre s'est inscrit au créneau après leur création (bandeau d'alerte et couleur dédiée).
- Une leçon générée par l'admin affiche **« Sujet à définir » / « Lieu à définir »** tant que le coach ne les a pas choisis.
- Détail de ses leçons : participants avec leur galop et leur cheval. Il ne voit pas les participants des leçons des autres coachs.
- **Sujet et lieu** : le coach les choisit et enregistre (uniquement sur une leçon prévue).
- **Attribution des chevaux** : pour chaque participant, la liste de tous les chevaux du centre s'affiche avec leur race, leur poids, leur niveau requis et leurs heures de travail du jour. Les chevaux non sélectionnables sont **grisés avec le motif** :
  - statut autre que « Disponible » (repos, au pré, convalescence) ;
  - niveau du cavalier inférieur au niveau requis par le cheval ;
  - cavalier pesant plus de **20 % du poids du cheval** ;
  - cheval déjà attribué à un autre cavalier sur le même horaire ;
  - nombre maximal d'heures de travail du cheval dans la journée atteint (**3 h au maximum**) ;
  - cheval ayant déjà travaillé **2 heures d'affilée** (une pause est obligatoire).
- Possibilité de changer ou de retirer le cheval d'un participant.

**Préparation des cours** (`/coach/preparation`)
- Ses leçons de la semaine sous forme de fiches, chacune avec son éditeur (sujet, lieu, chevaux).
- Alerte indiquant combien de cavaliers n'ont pas encore de cheval cette semaine.

### Admin

**Tableau de bord**
- **Demandes de modification de cours en attente** : nom du membre, cours actuel → cours souhaité, motif. L'admin **accepte** (le membre est déplacé dans le nouveau cours, les règles de places et de niveau sont revérifiées) ou **refuse**.

**Membres** (`/admin/membres`)
- Tableau (AG Grid) de tous les cavaliers : nom, email, galop, taille, poids.
- **Ajout d'un membre** : prénom, nom, email (unique), mot de passe provisoire (8 caractères minimum), galop (0 à 7), taille, poids.

**Chevaux** (`/admin/chevaux`)
- Tableau de la cavalerie : nom, race, statut, lieu actuel, taille, poids, heures de travail par jour, niveau requis.
- **Ajout, modification et suppression** d'un cheval. Les heures de travail par jour sont limitées à 3. Un cheval déjà attribué à des leçons ne peut pas être supprimé : il faut passer son statut à « Repos » ou « Convalescence ».

**Créneaux hebdomadaires** (`/admin/creneaux`)
- Calendrier de la semaine type : cliquer sur une case vide crée un créneau à ce jour et à cette heure (début à l'heure pile ou à la demi-heure).
- **Créer / modifier / supprimer** un créneau : coach, jour, heure de début (durée fixe d'1 heure), nombre de places (12 au maximum), niveau requis. Un coach ne peut pas avoir deux créneaux qui se chevauchent.
- **Gestion des inscrits** du créneau : liste des membres inscrits, ajout parmi les seuls membres éligibles (niveau suffisant, place disponible, pas d'autre créneau au même moment), retrait d'un membre.
- **Générer les leçons** jusqu'à une date (un an au maximum) :
  - une leçon par créneau et par semaine, d'aujourd'hui à la date choisie ;
  - les membres inscrits au créneau sont **ajoutés directement** comme participants ;
  - le sujet et le lieu restent **à définir par le coach** ;
  - les leçons déjà existantes ne sont pas modifiées, et une leçon n'est pas créée si son coach donne déjà un cours à cet horaire ;
  - un message récapitule le nombre de leçons créées, de participants ajoutés et de leçons ignorées.

**Toutes les leçons** (`/admin/lecons`)
- Le calendrier de toutes les leçons de la semaine, avec le coach et le nombre de participants de chacune.
- Sur n'importe quelle leçon, l'admin peut modifier :
  - le **coach** (refusé si ce coach donne déjà une leçon à cet horaire) ;
  - le **sujet** et le **lieu** (ou les laisser « À définir » pour le coach) ;
  - le **statut** : prévue, annulée ou terminée. **Annuler une leçon libère ses chevaux** ;
  - les **chevaux des participants**, avec les mêmes règles que pour les coachs.

### Pas encore disponible

- Inscription d'un membre à un créneau par lui-même (aujourd'hui, c'est l'admin qui inscrit).
- Création des comptes coachs depuis l'interface (possible uniquement par l'API, `POST /register`).
- Désinscription automatique d'un membre au bout d'un an.

## Lancer l'application

L'application se compose de trois éléments qui tournent en local :

| Élément | Technologie | Adresse |
|---|---|---|
| Base de données | PostgreSQL 17 (conteneur Docker) | `localhost:5433` |
| API (back) | Java 25, Spring Boot 4.1 | http://localhost:8080 |
| Interface (front) | Angular 22 | http://localhost:4200 |

### 1. Installer les prérequis

| Outil | Version | Vérifier | Rôle |
|---|---|---|---|
| **JDK** | 25 | `java -version` | Compiler et lancer le back |
| **Docker Desktop** (avec Docker Compose) | récent | `docker compose version` | Base PostgreSQL |
| **Node.js** | `^22.22.3`, `^24.15.0` ou `>=26` | `node -v` | Lancer le front |
| **npm** | 11 (fourni avec Node 24) | `npm -v` | Dépendances du front |
| **Git** | récent | `git --version` | Récupérer les dépôts |

Maven n'est **pas** à installer : le wrapper `mvnw` fourni le télécharge au premier lancement.

### 2. Récupérer les deux dépôts

```bash
git clone https://github.com/TristanBrouille/HorseCo-Pilote.git
git clone https://github.com/TristanBrouille/HorseCo-Pilote-Front.git
```

### 3. Lancer le back (base + API)

Depuis le dossier `HorseCo-Pilote/horsecopilote/` :

```bash
# Démarrer la base PostgreSQL (port hôte 5433)
docker compose up -d

# Télécharger les dépendances Maven et lancer l'API sur http://localhost:8080
./mvnw spring-boot:run          # Windows : .\mvnw.cmd spring-boot:run
```

- Docker Desktop doit être démarré. Si la base n'est pas lancée, Spring Boot démarre lui-même `compose.yaml` au lancement.
- Au premier lancement, Maven télécharge toutes les dépendances (Spring, Hibernate, Flyway, MapStruct, springdoc…), ce qui prend quelques minutes.
- Au démarrage, **Flyway crée le schéma** (`src/main/resources/db/migration`) et, avec le profil `dev` (par défaut en local), **insère les données de test** (`src/main/resources/db/dev-data`).
- L'API est prête quand la console affiche `Started HorsecopiloteApplication`.

### 4. Lancer le front

Dans un autre terminal, depuis le dossier `HorseCo-Pilote-Front/horsecopilote-front/` :

```bash
# Télécharger les dépendances (Angular, Angular Material, AG Grid…) : à refaire après chaque pull qui modifie package.json
npm install

# Lancer le serveur de développement sur http://localhost:4200 (rechargement automatique)
npm start
```

Ouvrir ensuite **http://localhost:4200** et se connecter avec un des [comptes de test](#comptes-de-test).

### Commandes utiles

| Commande | Dossier | Effet |
|---|---|---|
| `./mvnw test` | back | Tests du back (la base doit être lancée) |
| `./mvnw package` | back | Construit `target/horsecopilote-*.jar` |
| `npm test` | front | Tests du front (Vitest) |
| `npm run build` | front | Build de production dans `dist/` |
| `docker compose ps` | back | État du conteneur `horsecopilote-db` |
| `docker compose down` | back | Arrête la base (les données sont conservées) |
| `docker compose down -v` | back | Arrête la base **et supprime ses données** : elle repart des données de test au prochain lancement |
| `docker exec -it horsecopilote-db psql -U horse -d horsecopilote` | back | Console SQL |

Paramètres de la base : base `horsecopilote`, utilisateur `horse`, mot de passe `horse`, port hôte `5433`.

### Problèmes fréquents

- **`Connection refused` au démarrage du back** : Docker Desktop n'est pas lancé, ou la base n'est pas démarrée (`docker compose up -d`).
- **`Port 8080 was already in use`** : une autre instance du back tourne déjà (IDE, autre terminal). L'arrêter, ou lancer sur un autre port avec `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` (le front appelle toutefois le port 8080).
- **Erreur de validation Flyway / Hibernate** après un changement de migration : repartir d'une base vide avec `docker compose down -v` puis `docker compose up -d`.
- **Erreurs réseau ou CORS dans la console du navigateur** : le back n'est pas lancé, ou le front ne tourne pas sur le port 4200 (seule origine autorisée par le back, voir `app.cors.allowed-origins` dans `application.yaml`).
- **Module introuvable après un pull côté front** : relancer `npm install`.

## Comptes de test

Avec le profil `dev` (profil par défaut en local), la base est remplie au démarrage avec des données de test : 1 admin, 3 coachs, 8 membres (galop 1 à 7), 10 chevaux couvrant tous les statuts, 7 créneaux et les leçons de la **semaine du lundi 12 octobre 2026**.

**Mot de passe de tous les comptes : `password`**

| Rôle | Email | Mot de passe | À essayer |
|---|---|---|---|
| Admin | `admin@horseco.test` (Julie Bernard) | `password` | Créneaux, génération des leçons, toutes les leçons, membres, chevaux |
| Coach | `sophie.martin@horseco.test` (Sophie Martin) | `password` | Agenda de la semaine du 12/10/2026 : leçon du mercredi 17 h en surbrillance (nouvelle inscrite à valider) |
| Membre | `lea.petit@horseco.test` (Léa Petit, galop 1) | `password` | Mes cours de la semaine du 12/10/2026, avec ses chevaux attribués |

Autres comptes disponibles (même mot de passe) : coachs `claire.durand@horseco.test` et `marc.lefevre@horseco.test` ; membres `hugo.roux`, `chloe.moreau`, `tom.fournier`, `emma.girard`, `lucas.bonnet`, `ines.lambert`, `nathan.fontaine` (tous en `@horseco.test`).

> Les leçons de démonstration sont dans la semaine du 12/10/2026 : utilisez les flèches de navigation de l'agenda pour y revenir, ou générez de nouvelles leçons depuis l'écran des créneaux (compte admin).

## Documentation de l'API (Swagger)

Une fois le back lancé, la documentation interactive de l'API est disponible sur :

- **Swagger UI : http://localhost:8080/swagger-ui.html**
- Spécification OpenAPI (JSON) : http://localhost:8080/v3/api-docs

Elle liste tous les endpoints, regroupés par fonctionnalité (Authentification, Chevaux, Coachs, Créneaux hebdomadaires, Leçons, Membres, Mes cours, Demandes de modification), avec les paramètres et les corps de requête et de réponse.

**Essayer les endpoints depuis Swagger UI :**

1. Ouvrir l'endpoint `POST /login` (groupe *Authentification*), cliquer sur **Try it out** et envoyer :
   ```json
   { "email": "admin@horseco.test", "password": "password" }
   ```
2. Le serveur ouvre une session (cookie `JSESSIONID`) que le navigateur renvoie automatiquement : les autres endpoints peuvent alors être appelés avec les droits de ce compte.
3. Pour changer de rôle, rappeler `POST /login` avec un autre compte. La déconnexion (`POST /logout`) est gérée par Spring Security et n'apparaît donc pas dans la liste.

Codes de réponse communs : `401` sans session, `403` rôle insuffisant, `400` données invalides (détail par champ dans `errors`), `404` ressource introuvable, `409` règle métier non respectée. Toutes les erreurs sont renvoyées au format `ProblemDetail`.

La documentation est publique (lisible sans être connecté) ; les endpoints qu'elle décrit restent protégés.

## Tests

```bash
./mvnw test        # Windows : .\mvnw.cmd test
```

**193 tests** JUnit 5 couvrent les services (règles métier, avec Mockito), les mappers MapStruct, les controllers (`@WebMvcTest` avec la vraie configuration de sécurité) et le démarrage complet de l'application. Le test de démarrage utilise la base locale : lancer `docker compose up -d` avant.

Le détail (stratégie, organisation, liste des classes de test et règles métier couvertes) est dans la **[documentation des tests](horsecopilote/docs/TESTS.md)**.

## Notre utilisation de l'IA

### Ce qui a été fait avec l'IA

L'IA (Claude Code) a été utilisée de façon assez **généralisée sur le projet, principalement pour l'édition du code** : écriture des entités, services, controllers, DTO, composants Angular, styles et tests, à partir de ce que nous avions défini au préalable.

### Ce qui a été fait sans IA

- **Les parties importantes du code, notamment l'authentification et la sécurité**, ont été écrites par l'équipe.
- **Tout le reste a été décidé sans IA** :
  - les **conventions** de code et de nommage ;
  - les **bonnes pratiques** à respecter ;
  - l'**architecture** (3 couches côté back, un dossier par écran côté front) ;
  - le **workflow de l'application** : rôles, cycle de vie des créneaux et des leçons, règles métier ;
  - la **base de données** : modèle, tables et migrations.

Ces décisions ont été écrites dans des fichiers de contexte (`CLAUDE.md` et les « skills » du dossier `.claude/`) que l'IA suit pour chaque modification : ce sont elles qui cadrent le code produit.

### Notre ressenti

Confier l'édition du code à l'IA nous a surtout permis de **prendre plus de temps pour définir le projet en détail** : les règles métier, le modèle de données, les droits de chaque rôle et les choix techniques. Plutôt que de passer l'essentiel du temps à écrire du code, nous l'avons passé à réfléchir à ce que l'application devait faire et à justifier les choix faits.
