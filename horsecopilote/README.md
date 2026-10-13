# HorseCo-Pilote — Back (API REST)

API Spring Boot du CRM de gestion du centre équestre. Le front Angular est dans le dépôt [HorseCo-Pilote-Front](https://github.com/TristanBrouille/HorseCo-Pilote-Front).

## Prérequis

- **Java 25** (JDK) — `java -version`
- **Docker** + Docker Compose — pour la base PostgreSQL 17
- Maven n'est pas nécessaire : le wrapper `./mvnw` (ou `mvnw.cmd` sous Windows) le télécharge.

## Lancer le projet

Toutes les commandes se lancent depuis ce dossier (`horsecopilote/`).

```bash
# 1. Démarrer la base PostgreSQL (port hôte 5433)
docker compose up -d

# 2. Lancer l'API (http://localhost:8080) — Flyway applique les migrations au démarrage
./mvnw spring-boot:run
```

Sous Windows (PowerShell / cmd), remplacer `./mvnw` par `.\mvnw.cmd`.

Lancer ensuite le front (voir son README) : il appelle l'API sur `http://localhost:8080/api`, et le CORS autorise `http://localhost:4200`.

## Base de données

| Paramètre | Valeur |
|---|---|
| Hôte / port | `localhost:5433` |
| Base | `horsecopilote` |
| Utilisateur / mot de passe | `horse` / `horse` |

Le schéma est géré uniquement par Flyway (`src/main/resources/db/migration`), Hibernate ne fait que le valider.

```bash
docker compose ps        # état du conteneur horsecopilote-db
docker compose down      # arrêter la base (les données sont conservées)
docker compose down -v   # arrêter et SUPPRIMER les données (base repartie de zéro au prochain démarrage)
docker exec -it horsecopilote-db psql -U horse -d horsecopilote   # console SQL
```

## Tests et build

```bash
./mvnw test       # tests unitaires
./mvnw package    # construit target/horsecopilote-*.jar
```

## Problèmes fréquents

- **`Connection refused` au démarrage** : la base n'est pas lancée → `docker compose up -d`.
- **Port 5433 ou 8080 déjà utilisé** : arrêter le processus qui l'occupe, ou changer le port dans `compose.yaml` / `application.properties`.
- **Erreur de validation Flyway / Hibernate** après un changement de migration : repartir d'une base vide avec `docker compose down -v` puis `docker compose up -d`.
