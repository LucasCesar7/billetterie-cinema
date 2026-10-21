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
