# Compte Rendu - Backend Billetterie Cinéma (R5.08)

**Auteur :** Étudiant BUT 3 Informatique  
**Technologies :** Java 25, Spring Boot 4.1.1, MySQL 8, Flyway, Spring Security, Testcontainers  
**Outils IA :** Gemini 3.8 Flash (raisonnement élevé) + Argon via l'agent Antigravity (VS Code)

---

## 1. Stack technique et configuration de l'environnement

- **Java 25 :** Utilisation du compilateur avec le flag `release 25`. Aucun processeur d'annotation tiers (pas de Lombok) pour garantir une compatibilité totale sans plugin expérimental.
- **Spring Boot 4.1.1 :** Déclaré comme parent unique dans le `pom.xml`. Les versions des starters officiels ne sont pas redéclarées manuellement pour éviter les conflits de dépendances.
- **MySQL :** Déployé localement avec Docker Compose (port 3306). Aucun recours à H2 afin de tester dès le départ les contraintes SQL réelles et les verrous de concurrence.
- **Migrations Flyway :** Validation stricte (`ddl-auto: validate`) pour s'assurer que le code Java s'aligne sur le schéma SQL versionné et non l'inverse.

---

## 2. Démarche et pilotage de l'agent IA

Le développement est réalisé via l'agent Antigravity sous VS Code piloté par Gemini 3.8 Flash. Pour éviter les dérives de code et les hallucinations :
- Un fichier `.agentrules` fixe les règles strictes (code 100 % en français, records Java 25, zéro H2).
- Le projet avance par phases séquentielles vérifiées par `mvnw` avant chaque commit autonome.
- La conception des prompts et des contrats d'API est cadrée en amont.

### Journal des prompts structurants :
- **Prompt Phase 1 (Initialisation) :** Initialisation autonome du socle Maven (Java 25, Spring Boot 4.1.1, wrapper 3.3.4), configuration du fichier `docker-compose.yml` (MySQL 8.4 avec healthcheck), fichier `application.yml` avec validation stricte Hibernate et exposition Actuator minimale, arborescence Maven standard, classe principale et validation de la compilation par `mvnw clean compile`.
- **Prompt Phase 2 (Persistance & Schéma) :** Configuration du remote GitHub officiel (`LucasCesar7/billetterie-cinema`), migrations Flyway V1 (schéma DDL complet avec contraintes CHECK et clés étrangères) et V2 (jeux de données de test), entités JPA sans Lombok, repositories Spring Data (avec `findByIdWithLock` et calcul des places réservées), tests d'intégration Testcontainers MySQL et validation du schéma sous `ddl-auto: validate`.

---

## 3. Phase 1 : Initialisation du socle technique

- **Dépôt Git & Wrapper :** Dépôt Git initialisé sur la branche principale `main` avec `.gitignore` complet (exclusion de `target/`, fichiers d'IDE `.idea/`, `.vscode/`, `.env`). Mise en place du Maven Wrapper exécutable sous Windows et Linux (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`).
- **Configuration Maven & Java 25 :** Déclaration du parent `spring-boot-starter-parent` en version `4.1.1`, ciblage du compilateur sur la release 25 (`<maven.compiler.release>25</maven.compiler.release>` et `<java.version>25</java.version>`). Détection automatique du JDK 25 configuré sur la machine.
- **Dépendances intégrées :**
  - Socle & persistance : `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`, `spring-boot-starter-flyway`, `flyway-mysql`, `mysql-connector-j`.
  - Exploitation & documentation : `spring-boot-starter-actuator` (limité à `/health`), `springdoc-openapi-starter-webmvc-ui:3.1.1` (chemins `/v3/api-docs` et `/swagger-ui.html`).
  - Tests : `spring-boot-starter-test`, `spring-boot-starter-security-test`, `spring-boot-testcontainers`, `testcontainers-mysql`, `testcontainers-junit-jupiter`.
  - Respect strict des consignes : aucune dépendance superflue (zéro Lombok, zéro H2).
- **Docker Compose :** Service MySQL 8.4 configuré (`billetterie_db`, `cinema_user` / `cinema_pass`), port 3306 exposé, volume nommé persistant et healthcheck configuré via `mysqladmin ping`.
- **Configuration applicative (`application.yml`) :** Datasource pointant sur MySQL local, `spring.jpa.hibernate.ddl-auto: validate`, `spring.jpa.open-in-view: false`, Actuator configuré pour n'exposer que `health`.
- **Validation :** `.\mvnw.cmd clean compile` exécuté avec succès.

---

## 4. Phase 2 : Modèle relationnel, persistance JPA et intégrité des données

### 4.1. Architecture et choix de conception

- **Typage des données SQL & Java :**
  - **Montants monétaires (`DECIMAL(6,2)` / `BigDecimal`) :** Les prix unitaires sont modélisés avec `BigDecimal` côté Java et `DECIMAL(6,2)` côté SQL pour éviter les dérives d'arrondi inhérentes aux types IEEE 754 (`float`/`double`).
  - **Identifiants (`BIGINT` / `Long`) :** Utilisation systématique d'entiers 64 bits auto-incrémentés pour les clés primaires, prévenant tout épuisement d'espace d'adressage.
  - **Horodatages (`DATETIME` / `LocalDateTime`) :** Représentation précise des horaires de séance et de création des réservations.
- **Contraintes d'intégrité et unicité SQL :**
  - **Nom de salle unique (`salle.nom UNIQUE`) :** Interdit tout doublon de désignation physique dans le cinéma.
  - **Code de réservation unique (`reservation.code UNIQUE`) :** Garantit l'unicité stricte de chaque référence de réservation émise.
  - **Contraintes CHECK :** Cohérence garantie au plus près des données (durée de film entre 40 et 300 minutes, capacité de salle entre 20 et 500 places, prix unitaire positif, 1 à 8 places par réservation, statut parmi `CONFIRMEE` ou `ANNULEE`).
  - **Clés étrangères avec `ON DELETE RESTRICT` :** Protection stricte contre les suppressions orphelines (impossible de supprimer une salle ou un film rattaché à une séance existante).
- **Justification de l'absence de Lombok :**
  - Utilisation exclusive des fonctionnalités natives de Java 25 sans processeur d'annotations tiers.
  - Garantie d'indépendance vis-à-vis des modules internes de `javac` (aucun besoin de drapeaux `--add-opens` lors de la compilation).
  - Bytecode propre, constructeurs et accesseurs explicites facilitant le débogage et l'analyse statique.
- **Contrôle de concurrence (`findByIdWithLock`) :**
  - La méthode `findByIdWithLock` utilise `@Lock(LockModeType.PESSIMISTIC_WRITE)`, traduite en `SELECT ... FOR UPDATE` par MySQL InnoDB.
  - Cette stratégie repose directement sur la base de données pour sérialiser les réservations simultanées et garantir l'invariant de capacité même en environnement distribué ou multi-instances, là où une synchronisation mémoire échouerait.

### 4.2. Validation du schéma par Testcontainers

- Implémentation du test d'intégration `SchemaValidationTest` s'appuyant sur un conteneur officiel MySQL 8.4 via Testcontainers et `@ServiceConnection`.
- Validation réussie du démarrage complet avec `spring.jpa.hibernate.ddl-auto: validate`, de l'exécution séquentielle des scripts Flyway `V1` et `V2`, et de l'interrogation des repositories Spring Data.

---

## 4. Phase 2 : Modèle relationnel, persistance JPA et intégrité des données

### 4.1. Architecture et choix de conception

- **Typage des données SQL & Java :**
  - **Montants monétaires (`DECIMAL(6,2)` / `BigDecimal`) :** Les prix unitaires sont modélisés avec `BigDecimal` côté Java et `DECIMAL(6,2)` côté SQL pour éviter les dérives d'arrondi inhérentes aux types IEEE 754 (`float`/`double`).
  - **Identifiants (`BIGINT` / `Long`) :** Utilisation systématique d'entiers 64 bits auto-incrémentés pour les clés primaires, prévenant tout épuisement d'espace d'adressage.
  - **Horodatages (`DATETIME` / `LocalDateTime`) :** Représentation précise des horaires de séance et de création des réservations.
- **Contraintes d'intégrité et unicité SQL :**
  - **Nom de salle unique (`salle.nom UNIQUE`) :** Interdit tout doublon de désignation physique dans le cinéma.
  - **Code de réservation unique (`reservation.code UNIQUE`) :** Garantit l'unicité stricte de chaque référence de réservation émise.
  - **Contraintes CHECK :** Cohérence garantie au plus près des données (durée de film entre 40 et 300 minutes, capacité de salle entre 20 et 500 places, prix unitaire positif, 1 à 8 places par réservation, statut parmi `CONFIRMEE` ou `ANNULEE`).
  - **Clés étrangères avec `ON DELETE RESTRICT` :** Protection stricte contre les suppressions orphelines (impossible de supprimer une salle ou un film rattaché à une séance existante).
- **Justification de l'absence de Lombok :**
  - Utilisation exclusive des fonctionnalités natives de Java 25 sans processeur d'annotations tiers.
  - Garantie d'indépendance vis-à-vis des modules internes de `javac` (aucun besoin de drapeaux `--add-opens` lors de la compilation).
  - Bytecode propre, constructeurs et accesseurs explicites facilitant le débogage et l'analyse statique.
- **Contrôle de concurrence (`findByIdWithLock`) :**
  - La méthode `findByIdWithLock` utilise `@Lock(LockModeType.PESSIMISTIC_WRITE)`, traduite en `SELECT ... FOR UPDATE` par MySQL InnoDB.
  - Cette stratégie repose directement sur la base de données pour sérialiser les réservations simultanées et garantir l'invariant de capacité même en environnement distribué ou multi-instances, là où une synchronisation mémoire échouerait.

### 4.2. Validation du schéma par Testcontainers

- Implémentation du test d'intégration `SchemaValidationTest` s'appuyant sur un conteneur officiel MySQL 8.4 via Testcontainers et `@ServiceConnection`.
- Validation réussie du démarrage complet avec `spring.jpa.hibernate.ddl-auto: validate`, de l'exécution séquentielle des scripts Flyway `V1` et `V2`, et de l'interrogation des repositories Spring Data.

