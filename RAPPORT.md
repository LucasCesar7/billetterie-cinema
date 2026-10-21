# Rapport Technique et Méthodologique - Backend Billetterie Cinéma

**Auteur :** Lucas César  
**Cadre :** R5.08 - Qualité de Développement & Automatisation des Tests  
**Projet :** Système de Billetterie de Cinéma Haute Fiabilité  
**Stack :** Java 25 natif, Spring Boot 4.1.1, Spring Data JPA / Hibernate, Flyway, MySQL 8.4 (Testcontainers), Thymeleaf, OpenAPI 3.1, Micrometer  
**Dépôt GitHub :** [https://github.com/LucasCesar7/billetterie-cinema.git](https://github.com/LucasCesar7/billetterie-cinema.git)

---

## 1. Contexte et Objectifs du Projet

L'objectif de ce projet est la conception et l'industrialisation d'un système de billetterie de cinéma robuste, capable de résister à de fortes montées en charge concurrentielles tout en respectant scrupuleusement les contraintes métier (interdiction absolue de surréservation, intégrité des données, validation stricte à l'entrée et traçabilité des opérations).

Le développement a été réalisé de manière autonome et assistée par l'intelligence artificielle (agent Antigravity / Gemini 3.8 Flash), sous le contrôle permanent d'un ensemble de règles strictes formalisées dans le fichier `.agentrules` :
- Code, attributs, tables, colonnes et messages d'erreur rédigés à 100 % en français.
- Exploitation des fonctionnalités natives de Java 25 sans processeur d'annotations tiers (0 Lombok).
- Exclusion absolue des bases de données de simulation (pas de H2).
- Validation permanente sur conteneurs officiels MySQL 8.4 via Testcontainers.
- Démarche de livraison incrémentale guidée par les tests et le versionnement Git.

---

## 2. Démarche méthodologique et collaboration avec l'IA

Plutôt que de solliciter l'IA pour générer du code en masse sans contrôle, nous avons mis en place une méthodologie d'ingénierie dirigée par les spécifications et encadrée par des garde-fous.

```text
┌─────────────────────────────────┐
│   Conception humaine & Cadrage  │ (Arbitrages d'architecture, règles strictes)
└────────────────┬────────────────┘
                 ▼
┌─────────────────────────────────┐
│ Fichiers de contexte persistants│ (.agentrules, MEMOIRE.md)
└────────────────┬────────────────┘
                 ▼
┌─────────────────────────────────┐
│   Prompts modulaires étanches   │ (1 phase = 1 responsabilité fonctionnelle)
└────────────────┬────────────────┘
                 ▼
┌─────────────────────────────────┐
│ Exécution Agent (Antigravity)   │ (Implémentation sur branche Git dédiée)
└────────────────┬────────────────┘
                 ▼
┌─────────────────────────────────┐
│ Contrôle Maven & Testcontainers │ (.\mvnw clean verify avec MySQL 8.4 réel)
└────────────────┬────────────────┘
                 ▼
┌─────────────────────────────────┐
│  Validation humaine & Merge Git │ (Fast-forward sur main, archivage mémoire)
└─────────────────────────────────┘
```

### 2.1 Rôles respectés

- **Concepteur / Pilote (Humain) :** Définition des choix d'architecture (modèle de données, verrou pessimiste, absence de Lombok, séparation des chaînes de sécurité), rédaction des contrats de test, découpage en phases incrémentales et validation de chaque étape via le terminal.
- **Agent d'implémentation (Antigravity avec Gemini 3.8 Flash / Argon) :** Génération du code source Java 25 natif, écriture des migrations SQL Flyway, configuration Spring Security, rédaction des gabarits Thymeleaf et exécution des commandes Git et Maven en autonomie supervisée.

### 2.2 Garde-fous anti-régression

Pour éliminer les risques d'hallucination ou de dérive du code :
- **`.agentrules` :** Fichier racine fixant les règles absolues : variables et nommage 100 % en français, Java 25 natif (records obligatoires pour les DTOs, zéro processeur Lombok), MySQL strict (interdiction formelle de H2), et gestion Git autonome par branches.
- **`MEMOIRE.md` :** Registre d'état persistant que l'agent devait obligatoirement lire avant d'entamer une nouvelle tâche afin de maintenir l'alignement avec les classes existantes.
- **Workflow Git par branches :** Toute tâche est développée sur une branche `feature/...`, validée par une construction Maven propre (`.\mvnw.cmd clean verify`), puis fusionnée sur `main`.

---

## 3. Journal des Prompts directeurs et Pilotage de l'Agent

Cette section documente l'intégralité des prompts fournis à l'agent Antigravity. Chaque consigne a été formulée pour cadrer précisément les dépendances, les contraintes de types, les statuts HTTP et les scénarios de test.

### Phase 1 — Socle technique, Wrapper Maven et Conteneurisation MySQL

**Intention :** Poser une base saine et reproductible sans dépendance parasite, initialiser Git et Docker.

**Prompt transmis à l'agent :**
```text
Tu es chargé d'initialiser la Phase 1 du backend de billetterie de cinéma (Java 25, Spring Boot 4.1.1, MySQL).
Respecte scrupuleusement les consignes de .agentrules (variables en français, pas de Lombok, DTO en records, gestion Git autonome).

Voici les tâches à réaliser de manière autonome :
1. Configuration Git & GitHub : initialise le repo local si ce n'est pas fait (git init), configure la branche sur main, crée un .gitignore propre (ignorant target/, .idea/, .vscode/, .env).
2. Maven Wrapper & Arborescence : mets en place mvnw, mvnw.cmd et .mvn/wrapper/maven-wrapper.properties (v3.3.4), arborescence standard fr/univ/billetterie.
3. Fichier pom.xml :
   - Parent : org.springframework.boot:spring-boot-starter-parent version 4.1.1.
   - Propriétés : <java.version>25</java.version> et <maven.compiler.release>25</maven.compiler.release>.
   - Dépendances sans version explicite : starter-webmvc, starter-data-jpa, starter-validation, starter-security, starter-flyway, flyway-mysql (runtime), mysql-connector-j (runtime), starter-actuator.
   - Documentation : springdoc-openapi-starter-webmvc-ui:3.1.1.
   - Tests : starter-test, starter-security-test, spring-boot-testcontainers, testcontainers-mysql, testcontainers-junit-jupiter.
   - Plugin : spring-boot-maven-plugin dans build/plugins. Zéro Lombok, zéro H2.
4. Docker Compose : mysql:8.4, base billetterie_db, utilisateur cinema_user/cinema_pass, port 3306, healthcheck actif.
5. application.yml : ddl-auto: validate, open-in-view: false, actuator limité à /health.
6. Classe principale BilletterieApplication.java (@SpringBootApplication).
7. Validation : lance .\mvnw.cmd clean compile. Si succès, commit : feat(init): configuration initiale maven java 25, docker mysql et wrapper.
```

### Phase 2 — Schéma Flyway, Entités JPA Java 25 et Repositories

**Intention :** Créer le schéma SQL strict sous Flyway avec les contraintes imposées (CHECK, UNIQUE), mapper les entités en Java natif sans Lombok et valider la correspondance avec `ddl-auto: validate` via Testcontainers.

**Prompt transmis à l'agent :**
```text
Tu passes à la Phase 2 : Configuration remote GitHub, Migrations Flyway, Entités JPA et Repositories.
Respecte scrupuleusement .agentrules (Java 25 natif, 0 Lombok, nommage en français, MySQL strict).
Dépôt distant : https://github.com/LucasCesar7/billetterie-cinema.git

Tâches :
1. Synchronisation Git : configure origin, pousse la branche main.
2. Migrations Flyway (db/migration/) :
   - V1__creation_schema.sql :
     * table film : id (BIGINT PK AI), titre (VARCHAR NOT NULL), genre, duree_minutes (CHECK 40-300), date_sortie (DATE).
     * table salle : id, nom (VARCHAR UNIQUE), capacite (CHECK 20-500).
     * table seance : id, film_id (FK), salle_id (FK), debut (DATETIME), prix_unitaire (DECIMAL(6,2) CHECK > 0). Index sur debut, film_id, salle_id.
     * table reservation : id, code (VARCHAR UNIQUE), seance_id (FK), email_client, nombre_places (CHECK 1-8), statut (CHECK 'CONFIRMEE','ANNULEE'), creee_le (DATETIME).
   - V2__donnees_demonstration.sql : 2 films ("Inception", "Interstellar"), 1 salle ("Salle 1", 100 places), 1 séance future à 10.50€.
3. Modèle JPA (fr.univ.billetterie.entity) :
   - Enum StatutReservation (CONFIRMEE, ANNULEE).
   - Entités Film, Salle, Seance, Reservation en Java 25 classique (constructeurs explicites, getters/setters, BigDecimal pour prix).
   - Relations @ManyToOne(fetch = FetchType.LAZY, optional = false).
4. Repositories (fr.univ.billetterie.repository) :
   - FilmRepository : recherche par titre/genre insensible à la casse.
   - SalleRepository : findByNom, existsByNom.
   - SeanceRepository : findByDebutBetween, et méthode de verrouillage :
     @Lock(LockModeType.PESSIMISTIC_WRITE)
     @Query("SELECT s FROM Seance s WHERE s.id = :id")
     Optional<Seance> findByIdWithLock(@Param("id") Long id);
   - ReservationRepository : findByCode, et sumPlacesReservees avec COALESCE.
5. Test de validation SchemaValidationTest avec @DataJpaTest et Testcontainers MySQL 8.4 pour valider ddl-auto: validate.
6. Validation par .\mvnw.cmd test, commit feat(persistance): schema flyway v1/v2, entites jpa java 25 et repositories, push origin main.
```

### Phase 3 — Contrat REST, DTO Records, Exceptions et Sécurité HTTP Basic

**Intention :** Établir l'API REST avec contrôleurs fins, DTOs immuables, gestion d'erreur centralisée (400, 404, 409) et sécurité HTTP Basic par rôles (CLIENT, PROGRAMMATEUR, ADMIN).

**Prompt transmis à l'agent :**
```text
Tu passes à la Phase 3 : Fichier MEMOIRE.md, branche Git dédiée, DTOs records, exceptions, ControllerAdvice, contrôleurs REST et sécurité HTTP Basic.
Règles : Java 25 natif, 0 Lombok, 100% français, format JSON d'erreur strict du sujet.

Tâches :
1. Crée MEMOIRE.md consignant l'état du projet. Crée la branche feature/phase3-api-rest.
2. DTOs immuables (record Java natifs avec Jakarta Validation) :
   - Entrées : FilmCreationRequest, SalleCreationRequest, SeanceCreationRequest, ReservationCreationRequest.
   - Sorties : FilmResponse, SalleResponse, SeanceResponse, ReservationResponse, ErreurResponse (timestamp, status, code, message, path).
3. Exceptions & Handler :
   - RessourceIntrouvableException (404), CapaciteInsuffisanteException (409), ConflitMetierException (409).
   - GlobalExceptionHandler (@RestControllerAdvice) gérant MethodArgumentNotValidException (400, "VALIDATION_ECHOUEE") et les exceptions métier sans divulguer de stacktrace.
4. Services métier (@Transactional) :
   - ReservationService : réservation avec findByIdWithLock, contrôle de capacité restante, code UUID unique, calcul total en BigDecimal, et annulation avec libération des places.
5. Sécurité HTTP Basic (SecurityConfig) :
   - BCrypt, 3 utilisateurs en mémoire : client/client123 (CLIENT), programmateur/prog123 (PROGRAMMATEUR), admin/admin123 (ADMIN).
   - Filtrage : routes publiques (/api/films, /api/seances, /actuator/health, openapi), /api/programmation/** (PROGRAMMATEUR, ADMIN), /api/admin/** (ADMIN), /api/reservations (CLIENT).
6. Contrôleurs REST retournant 200, 201 avec en-tête Location, ou 204.
7. Tests d'intégration validant les recettes A3, A4, A5, A6, A7, A9 sous Testcontainers.
8. Exécution .\mvnw.cmd test, merge sur main, push GitHub, mise à jour MEMOIRE.md et RAPPORT.md.
```

### Phase 4 — Invariant de capacité sous concurrence multi-thread (Recette A8)

**Intention :** Valider formellement la résistance aux réservations simultanées en base réelle sans surréservation via des threads parallèles.

**Prompt transmis à l'agent :**
```text
Tu passes à la Phase 4 : Invariant de capacité, gestion transactionnelle sous concurrence et test multi-threadé Testcontainers (Recette A8).
Règles : Java 25 natif, 0 Lombok, branche feature/phase4-concurrence-tests.

Tâches :
1. Audit du verrou : confirme l'annotation @Lock(LockModeType.PESSIMISTIC_WRITE) sur SeanceRepository.findByIdWithLock et @Transactional sur ReservationService.creerReservation.
2. Dimensionne le pool HikariCP (maximum-pool-size: 20) pour supporter la charge multi-thread.
3. Crée ReservationConcurrenceIntegrationTest sous Testcontainers MySQL 8.4 :
   - Test 1 (Scénario obligatoire du sujet) : séance avec 5 places restantes. 2 requêtes simultanées de 4 places déclenchées via CountDownLatch. Assertion : exactement un HTTP 201, exactement un HTTP 409 (CAPACITE_INSUFFISANTE), cumul final en base égal à 19/20 places (jamais 23).
   - Test 2 (Stress test) : séance avec 10 places restantes, 10 requêtes concurrentes de 2 places. Assertion : 5 succès (201), 5 rejets (409), cumul final de 20/20 places (zéro surréservation).
4. Vérification via .\mvnw.cmd clean verify.
5. Commit, merge sur main, push GitHub, mise à jour MEMOIRE.md et RAPPORT.md (justification technique InnoDB SELECT ... FOR UPDATE vs @Version).
```

### Phase 5 — Exploitation, OpenAPI, Bonus Micrometer et Packaging

**Intention :** Exposer l'interface Swagger UI, limiter Actuator à la santé, intégrer des compteurs d'exploitation et documenter la mise en service.

**Prompt transmis à l'agent :**
```text
Tu passes à la Phase 5 : Exploitation, Documentation OpenAPI/Actuator, Bonus Métier Micrometer, README.md et validation du livrable (Recettes A1, A2, A10).
Règles : branche feature/phase5-exploitation-bonus-readme.

Tâches :
1. Actuator : exposition publique restreinte à /actuator/health (UP).
2. SpringDoc OpenAPI 3.1.1 : OpenApiConfig avec métadonnées et composant SecurityScheme HTTP Basic. Vues /v3/api-docs et /swagger-ui.html publiques.
3. Bonus Métier Micrometer : injecte MeterRegistry dans ReservationService. Compteurs billetterie.places.reservees et billetterie.places.refusees incrémentés à chaque succès/rejet.
4. ExploitationIntegrationTest : vérifie health 200, openapi 200, swagger 200, et incrémentation des compteurs Micrometer.
5. Rédige README.md complet (contexte, démarche IA, prérequis JDK 25/Docker, commandes Maven, comptes de test, tableau des routes, décision de concurrence).
6. Packaging : exécute .\mvnw.cmd clean verify, vérifie le JAR autonome dans target/.
7. Merge sur main, push GitHub, mise à jour MEMOIRE.md et RAPPORT.md.
```

### Phase 6 — Interface Web Thymeleaf et Visualisation

**Intention :** Offrir une interface utilisateur fonctionnelle pour réserver et consulter les jauges de salle en direct sans casser les routes REST.

**Prompt transmis à l'agent :**
```text
Tu passes à la Phase 6 : Implémentation du front-end avec Thymeleaf, interface utilisateur sobre et artisanale, sans casser l'API REST ni les recettes A1 à A10.
Règles : branche feature/phase6-frontend-thymeleaf.

Tâches :
1. Ajoute spring-boot-starter-thymeleaf dans pom.xml (sans version explicite).
2. Ajuste SecurityConfig pour autoriser l'accès aux vues web (/, /films, /seances/**, /reserver/**, /confirmation/**, /annuler/**, /admin/tableau-de-bord, /css/**). Conserve HTTP Basic pour les tests REST.
3. Modèles de vue (records dans fr.univ.billetterie.dto.web) avec requêtes JOIN FETCH pour éviter tout LazyInitializationException.
4. CinemaWebController (@Controller) :
   - GET / : catalogue avec horaires, tarifs et disponibilités.
   - GET & POST /reserver : page de réservation avec calcul direct en JS et gestion d'alerte en cas de dépassement.
   - GET /confirmation/{code} : billet numérique et annulation.
   - GET /admin/tableau-de-bord : supervision en direct des compteurs Micrometer et des jauges d'occupation.
5. Styles CSS (style.css) : thème sombre cinéma (#0b0f17, #151d2a, or #eab308, vert #10b981, rouge #ef4444).
6. Tests web CinemaWebControllerTest. Exécution globale .\mvnw.cmd clean verify (20 tests au vert).
7. Merge sur main, push GitHub, mise à jour MEMOIRE.md et README.md.
```

### Phase 7 — Authentification Web (Form Login) et Connexion 1-Clic

**Intention :** Permettre de naviguer et tester les différents rôles (Client, Programmateur, Admin) directement dans le navigateur tout en conservant le statut 401 sur l'API REST.

**Prompt transmis à l'agent :**
```text
Tu passes à la Phase 7 : Interface de connexion Web, session utilisateur, boutons de test 1-clic et double SecurityFilterChain.
Règles : branche feature/phase7-authentification-web-login, zéro régression sur les 20 tests REST.

Tâches :
1. Ajoute thymeleaf-extras-springsecurity6 dans pom.xml.
2. Architecture Double SecurityFilterChain dans SecurityConfig :
   - Chaîne 1 (@Order(1)) sur /api/** : 100% Stateless, CSRF off, HTTP Basic. Renvoie STRICTEMENT 401 si anonyme (aucun redirect 302).
   - Chaîne 2 (@Order(2)) sur /** : session web, CSRF adapté, Form Login (/connexion), Logout (/deconnexion).
3. Vue /connexion : formulaire sobre avec boutons "Tester en tant que Client", "Tester en tant que Programmateur", "Tester en tant qu'Admin" remplissant et validant instantanément le formulaire en JavaScript.
4. Barre de navigation dynamique dans layout.html : affichage du nom et badge de rôle ([ADMIN], [CLIENT]), liens contextuels vers la gestion.
5. Écrans d'administration simples : /programmation (ajout film/séance), /admin/salles (création salle).
6. Validation par tests automatisés, .\mvnw.cmd clean verify, merge sur main et push GitHub.
```

### Phase 8 — CRUD Complet (Films, Salles, Séances) et Intégrité Référentielle

**Intention :** Permettre la modification et la suppression de chaque entité métier tout en interceptant les contraintes d'intégrité relationnelles SQL (`ON DELETE RESTRICT`).

**Prompt transmis à l'agent :**
```text
Tu passes à la Phase 8 : CRUD complet (Consultation, Modification, Suppression) pour Films, Salles et Séances (API REST et Web Thymeleaf).
Règles : branche feature/phase8-crud-complet, protection des clés étrangères ON DELETE RESTRICT.

Tâches :
1. DTOs de mise à jour : FilmModificationRequest, SalleModificationRequest, SeanceModificationRequest.
2. Couche Service avec intégrité métier :
   - FilmService.supprimerFilm : vérifie via seanceRepository.existsByFilmId. Si des séances existent, lève ConflitMetierException (409).
   - SalleService.modifierSalle (unicité du nom hors id courant) et supprimerSalle (bloqué si séances associées).
   - SeanceService.supprimerSeance (bloqué si réservations existantes).
3. Endpoints REST PUT et DELETE sur /api/programmation/films/{id}, /api/admin/salles/{id}, /api/programmation/seances/{id}.
4. Interface Web : boutons Modifier/Supprimer avec confirmations et alertes de refus dans gestion-salles.html et gestion-programmation.html.
5. Tests d'intégration du CRUD et de rejet en cas de conflit relationnel.
6. Validation .\mvnw.cmd clean verify, merge sur main, push GitHub, mise à jour README.md et RAPPORT.md.
```

---

## 4. Architecture logicielle et Choix techniques

```text
                         Navigateur Web (Thymeleaf)           Client HTTP / Test / Swagger
                                     │                                      │
                                     ▼                                      ▼
                        [ SecurityFilterChain Web ]            [ SecurityFilterChain REST ]
                                (@Order(2))                            (@Order(1))
                                     │                                      │
                                     ▼                                      ▼
                         [ CinemaWebController ]                [ Contrôleurs REST fins ]
                                     │                                      │
                                     └──────────────────┬───────────────────┘
                                                        │
                                                        ▼
                                              [ Services Métier ]
                                           (@Transactional, Micrometer)
                                                        │
                                                        ▼
                                             [ Repositories JPA ]
                                          (PESSIMISTIC_WRITE, JOIN FETCH)
                                                        │
                                                        ▼
                                             [ MySQL 8.4 InnoDB ]
                                        (Contraintes SQL, Verrous de ligne)
```

### 4.1. Approche en couches étanches et records Java 25
Le backend applique une séparation stricte des responsabilités :
- **Couche Présentation (REST & Web) :** Réception des requêtes HTTP, validation syntaxique Jakarta (`@Valid`), délégation immédiate à la couche service et conversion vers des modèles de vue ou DTOs immuables.
- **Couche Métier (Service) :** Frontière transactionnelle (`@Transactional`), délimitation des règles de gestion, application des verrous d'isolation, vérification préventive des dépendances avant suppression et incrémentation des compteurs d'exploitation.
- **Couche Persistance (Repository & JPA) :** Accès aux données relationnelles via Spring Data JPA avec requêtes optimisées `JOIN FETCH` pour supprimer les risques de requêtes N+1 et de `LazyInitializationException` en dehors de la session Hibernate (`open-in-view: false`).

### 4.2. Remplacement intégral de Lombok par Java 25 natif
Conformément aux directives de `.agentrules`, aucune dépendance à Lombok n'a été introduite :
- Les DTOs d'entrée, de modification, de sortie et les modèles de vue pour Thymeleaf sont déclarés sous la forme de `record` Java natifs, garantissant l'immuabilité des structures de données, la génération automatique des constructeurs compacts, des accesseurs, de `equals`, `hashCode` et `toString`.
- Les entités JPA (`Film`, `Salle`, `Seance`, `Reservation`) conservent des constructeurs explicites et des accesseurs standards, assurant une parfaite compatibilité avec les spécifications Jakarta Persistence sans aucune magie de bytecode.

### 4.3. Architecture de Sécurité : Double SecurityFilterChain
Afin de combiner harmonieusement une API REST consommée de façon programmatique et une application web interactive avec interface utilisateur dans le navigateur :
1. **Chaîne API REST (`@Order(1)`) :**
   - Cible exclusivement `/api/**` (`.securityMatcher("/api/**")`).
   - Mode sans état strict (`SessionCreationPolicy.STATELESS`), CSRF désactivé, authentification HTTP Basic activée.
   - En cas d'appel non authentifié, le point d'entrée renvoie un strict code `401 Unauthorized` (sans aucune redirection 302 vers une page de login).
   - Autorisations fines : consultation publique des films et séances, création/modification/suppression de films et séances pour `PROGRAMMATEUR` et `ADMIN`, gestion des salles pour `ADMIN`, réservations pour `CLIENT` et `ADMIN`.
2. **Chaîne Web UI (`@Order(2)`) :**
   - Cible l'ensemble des routes web (`/**`).
   - Active le Form Login (`/connexion`) et la déconnexion (`/deconnexion`).
   - Redirige automatiquement tout visiteur non authentifié (302) vers `/connexion` lors d'une tentative d'accès aux routes protégées (`/admin/**`, `/programmation/**`).
   - Intègre les tags de sécurité Thymeleaf (`sec:authorize`) pour adapter dynamiquement la barre de navigation selon le profil connecté.

---

## 5. Stratégie de Persistance et Gestion de la Concurrence

### 5.1. Schéma relationnel strict et intégrité référentielle
Le schéma SQL a été entièrement conçu pour garantir l'intégrité référentielle et relationnelle directement au niveau de la base MySQL :
- Clés primaires `BIGINT AUTO_INCREMENT` et clés étrangères protégées par `ON DELETE RESTRICT` (interdisant la suppression accidentelle d'une salle ou d'un film ayant des séances rattachées, ou d'une séance ayant des réservations).
- Contraintes d'intégrité déclaratives `CHECK` sur les durées, capacités, prix unitaires et nombres de places.
- Contraintes d'unicité sur le nom des salles physiques (`salle.nom`) et sur le code officiel de réservation (`reservation.code`).
- La configuration Hibernate `spring.jpa.hibernate.ddl-auto: validate` impose une synchronisation rigoureuse entre les scripts Flyway et les métadonnées JPA.
- En couche service, les méthodes de suppression effectuent un contrôle d'existence préventif (`existsByFilmId`, `existsBySalleId`, `existsBySeanceId`) pour convertir les blocages d'intégrité en exceptions métier explicites (`ConflitMetierException` code HTTP 409), évitant toute fuite de trace SQL.

### 5.2. Invariant de capacité sous contention (Recette A8)
Pour éliminer le risque de surréservation lors de requêtes concurrentes :
1. **Échec de l'approche optimiste (`@Version`) :** Sous forte charge, un verrouillage optimiste rejetterait massivement les réservations avec des erreurs `OptimisticLockException`, imposant une boucle de retry complexe et coûteuse pour les clients.
2. **Adoption du verrouillage pessimiste (`PESSIMISTIC_WRITE`) :** L'appel `seanceRepository.findByIdWithLock(seanceId)` génère la clause SQL `SELECT ... FOR UPDATE` sur la ligne ciblée de la table `seance` sous le moteur de stockage MySQL InnoDB.
3. **Atomicité garantie :** Les transactions concurrentes sont ordonnancées de manière déterministe. Chaque transaction calcule la somme exacte des places déjà confirmées (`sumPlacesReservees`). Si la capacité de la salle est insuffisante, l'exception `CapaciteInsuffisanteException` est immédiatement levée, déclenchant un `ROLLBACK` intégral sans aucune écriture partielle en base.

---

## 6. Matrice Complète des Recettes de Validation (A1 à A10 + Suites Web & CRUD)

Toutes les recettes du cahier des charges ainsi que l'interface utilisateur web, la sécurité et le CRUD complet ont été validées par des tests automatisés exécutés sur conteneur officiel MySQL 8.4 via Testcontainers :

| Recette / Domaine | Description | Scénario de test / Preuve technique | Classe de test associée | Statut |
| :--- | :--- | :--- | :--- | :--- |
| **A1** | **Compilation & Démarrage Maven** | Compilation sans warning sous Java 25 natif, packaging JAR exécutable autonome (`clean compile`, `clean package`). | Maven Wrapper / Root | **Validé** |
| **A2** | **Schéma relationnel & Migrations Flyway** | Validation stricte `ddl-auto: validate` sur MySQL 8.4 avec scripts `V1` (schéma, CHECK, FK) et `V2` (données initiales). | `SchemaValidationTest` | **Validé** |
| **A3** | **Consultation publique des films** | Requête anonyme `GET /api/films` renvoyant HTTP 200 avec la liste des films au catalogue. | `BilletterieIntegrationTest` | **Validé** |
| **A4** | **Validation des entrées & Erreur 400** | Requête `POST /api/films` avec un film de durée invalide (< 40 min) renvoyant HTTP 400 et code `VALIDATION_ECHOUEE`. | `BilletterieIntegrationTest` | **Validé** |
| **A5** | **Contrôle d'accès & Sécurité HTTP Basic** | `POST /api/salles` : anonyme rejeté (401), rôle `CLIENT` refusé (403), rôle `ADMIN` accepté (201). | `BilletterieIntegrationTest` | **Validé** |
| **A6** | **Réservation nominale** | Réservation de 4 places par un client authentifié (HTTP 201, code unique `RES-XXXXXXXXXX`, montant exact). | `BilletterieIntegrationTest` | **Validé** |
| **A7** | **Refus de surréservation** | Tentative de dépassement de la capacité restante de la salle renvoyant HTTP 409 et code `CAPACITE_INSUFFISANTE`. | `BilletterieIntegrationTest` | **Validé** |
| **A8** | **Concurrence & Invariant multi-thread** | 1. 5 places restantes, 2 requêtes simultanées de 4 places : exactement 1 succès (201) et 1 refus (409), total 19/20 (jamais 23).<br>2. Stress test : 10 places restantes, 10 requêtes de 2 places : 5 succès (201) et 5 refus (409), total 20/20 (0 surréservation). | `ReservationConcurrenceIntegrationTest` | **Validé** |
| **A9** | **Annulation & Libération de capacité** | Annulation d'une réservation (HTTP 204), statut passant à `ANNULEE` et libération immédiate des places pour une nouvelle réservation. | `BilletterieIntegrationTest` | **Validé** |
| **A10** | **Exploitation & Documentation publique** | 1. `GET /actuator/health` public renvoyant `{"status":"UP"}`.<br>2. `GET /v3/api-docs` public avec spécification OpenAPI.<br>3. `GET /swagger-ui.html` public (200 / 302).<br>4. Incrémentation vérifiée des compteurs Micrometer (`places.reservees` et `places.refusees`). | `ExploitationIntegrationTest` | **Validé** |
| **CRUD REST** | **Cycle de vie complet REST (Films, Salles, Séances)** | 1. Modification valide film (`PUT /api/programmation/films/{id}` -> 200).<br>2. Suppression film sans séance (`DELETE` -> 204).<br>3. Refus suppression salle avec séances (`DELETE` -> 409 `IMPOSSIBLE_SUPPRIMER_SALLE`).<br>4. Modification salle valide (`PUT /api/admin/salles/{id}` -> 200).<br>5. Suppression salle sans séance (`DELETE` -> 204).<br>6. Modification séance valide (`PUT /api/programmation/seances/{id}` -> 200).<br>7. Suppression séance sans réservation (`DELETE` -> 204).<br>8. Refus suppression séance avec réservations actives (`DELETE` -> 409 `IMPOSSIBLE_SUPPRIMER_SEANCE`). | `BilletterieIntegrationTest` | **Validé** |
| **Web UI & Auth** | **Interface Utilisateur, Form Login, CRUD Web** | 1. Affichage du catalogue public (`GET /` et `/films`).<br>2. Affichage page de connexion (`GET /connexion`).<br>3. Affichage formulaire séance (`GET /seances/{id}/reserver`).<br>4. Réservation nominale avec redirection 3xx (`POST /reserver`).<br>5. Rejet surréservation 200 avec message d'alerte.<br>6. Affichage billet numérique (`GET /confirmation/{code}`).<br>7. Annulation avec redirection 3xx (`POST /annuler/{code}`).<br>8. Redirection 3xx vers `/connexion` pour anonymes sur `/admin/tableau-de-bord`.<br>9. Accès 200 au tableau de bord pour `ADMIN`.<br>10. Accès 200 à la gestion de programmation pour `PROGRAMMATEUR`.<br>11. Accès 200 à la gestion des salles pour `ADMIN`.<br>12. Formulaire et édition de salle (`GET`/`POST` `/admin/salles/{id}/modifier`).<br>13. Suppression de salle isolée ou blocage si séances.<br>14. Formulaire et édition de film (`GET`/`POST` `/programmation/films/{id}/modifier`).<br>15. Suppression de film isolé ou blocage si séances.<br>16. Formulaire et édition de séance (`GET`/`POST` `/programmation/seances/{id}/modifier`).<br>17. Suppression de séance isolée ou blocage si réservations. | `CinemaWebControllerTest` | **Validé** |

**Bilan d'exécution final :** **46 tests d'intégration exécutés avec succès** (`.\mvnw.cmd clean verify`), 0 échec, 0 erreur (`BUILD SUCCESS`).

---

## 7. Retour d'Expérience sur la Collaboration avec l'IA

### 7.1. Ce qui a fonctionné efficacement :
1. **Le cadrage strict par `.agentrules` :** Définir en amont les interdits absolus (pas de Lombok, code 100 % en français, zéro H2) a totalement supprimé les hallucinations courantes où l'IA propose par défaut du code en anglais ou des dépendances comme H2.
2. **La mémoire persistante (`MEMOIRE.md`) :** En consignant l'état du système et les décisions techniques à chaque fin de phase, l'agent n'a jamais régressé sur les choix d'architecture antérieurs (ex. conservation systématique du verrou pessimiste, des DTOs en records et des filtres de sécurité).
3. **Le découpage par phases et les tests de recette :** L'approche "une phase = une branche Git = des tests Testcontainers = un commit" a permis de valider le comportement réel sous MySQL 8.4 au fur et à mesure, rendant les phases d'extension rapides et sans risque.

### 7.2. Pièges évités :
1. **La séparation des chaînes de sécurité :** Éviter l'écueil classique où l'ajout d'une page de login web casse les tests REST existants en renvoyant des redirections HTTP 302 au lieu de codes `401 Unauthorized`. L'utilisation ordonnée de deux chaînes (`@Order(1)` et `@Order(2)`) a permis d'isoler parfaitement les deux contextes d'exécution.
2. **Le verrouillage sous concurrence :** L'IA aurait pu être tentée d'utiliser une synchronisation Java en mémoire (`synchronized`). Le cadrage a imposé la délégation de la concurrence à InnoDB via `SELECT ... FOR UPDATE`, garantissant l'intégrité même en contexte distribué.
3. **La gestion de l'intégrité référentielle en suppression :** L'interception proactive en couche service des dépendances étrangères avant suppression évite de remonter des violations SQL brutes (`DataIntegrityViolationException`) et permet de renvoyer un code HTTP 409 propre et explicite pour le client de l'API.
