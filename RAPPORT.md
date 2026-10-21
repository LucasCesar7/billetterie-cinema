# Compte Rendu - Backend Billetterie Cinéma (R5.08)

**Auteur :** Lucas César - Étudiant BUT 3 Informatique  
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

### 2.1. Journal des prompts structurants :
- **Prompt Phase 1 (Initialisation) :** Initialisation autonome du socle Maven (Java 25, Spring Boot 4.1.1, wrapper 3.3.4), configuration du fichier `docker-compose.yml` (MySQL 8.4 avec healthcheck), fichier `application.yml` avec validation stricte Hibernate et exposition Actuator minimale, arborescence Maven standard, classe principale et validation de la compilation par `mvnw clean compile`.
- **Prompt Phase 2 (Persistance & Schéma) :** Configuration du remote GitHub officiel (`LucasCesar7/billetterie-cinema`), migrations Flyway V1 (schéma DDL complet avec contraintes CHECK et clés étrangères) et V2 (jeux de données de test), entités JPA sans Lombok, repositories Spring Data (avec `findByIdWithLock` et calcul des places réservées), tests d'intégration Testcontainers MySQL et validation du schéma sous `ddl-auto: validate`.
- **Prompt Phase 3 (API REST, DTOs, Gestion d'erreurs, Sécurité & Recette) :** Création du fichier de mémoire persistante `MEMOIRE.md`, DTOs immuables en records Java 25 natifs avec validation Jakarta, exceptions métier et `@RestControllerAdvice` centralisé (`ErreurResponse` sans fuite de stack trace ni détails SQL), services métier transactionnels avec verrouillage pessimiste, sécurité stateless HTTP Basic avec hachage BCrypt et 3 rôles (`CLIENT`, `PROGRAMMATEUR`, `ADMIN`), contrôleurs REST conformes aux spécifications HTTP, tests d'intégration complets couvrant les recettes A3, A4, A5, A6, A7 et A9 sous Testcontainers MySQL 8.4.
- **Prompt Phase 4 (Invariant de capacité, concurrence & Recette A8) :** Audit de la gestion transactionnelle, renforcement de l'isolation par verrou pessimiste InnoDB (`SELECT ... FOR UPDATE`), dimensionnement du pool HikariCP, conception du test multi-threadé `ReservationConcurrenceIntegrationTest` avec Testcontainers MySQL 8.4 validant le scénario nominal de concurrence (5 places restantes, 2 x 4 places demandées -> 1 succès 201, 1 refus 409) et le test de stress concurrentiel (10 places restantes, 10 x 2 places demandées -> 5 succès 201, 5 refus 409, 0 surréservation).

### 2.2. Stratégie d'isolation et gestion de la concurrence

#### Choix du verrou pessimiste (`PESSIMISTIC_WRITE`) face au verrou optimiste (`@Version`) :
- **Verrou optimiste (`@Version`) :** Repose sur la détection a posteriori d'un conflit lors du `COMMIT`. En cas de forte contention (ex. ouverture d'une billetterie pour un film très attendu), plusieurs transactions lisent simultanément la même version. La première valide sa réservation et incrémente la version ; toutes les autres transactions concurrentes sont brutalement rejetées avec une `OptimisticLockException`. Pour offrir une expérience utilisateur correcte, l'application devrait alors implémenter une boucle de retry complexe (réessayer la réservation, relire la capacité, recalculer la somme), augmentant la latence et la charge CPU du serveur applicatif sans garantie de succès.
- **Verrou pessimiste (`PESSIMISTIC_WRITE`) :** Repose sur la sérialisation des accès en amont directement au niveau du SGBD. La requête `SELECT ... FROM seance WHERE id = ? FOR UPDATE` pose un verrou exclusif sur la ligne de la séance ciblée dès l'entrée dans la méthode transactionnelle. Les transactions concurrentes se mettent en file d'attente au niveau du moteur de stockage plutôt que d'échouer. Chaque transaction examine séquentiellement l'état exact des places restantes. Dès que la capacité est atteinte, la transaction suivante détecte immédiatement que `placesDemandées > placesRestantes` et lève de manière déterministe `CapaciteInsuffisanteException` (HTTP 409), sans aucun besoin de retry applicatif.

#### Comportement de MySQL InnoDB (Verrou de ligne et atomicité) :
- MySQL InnoDB utilise des verrous de ligne d'index (*record locks*). Lorsqu'une transaction exécute `findByIdWithLock`, InnoDB acquiert un verrou exclusif (X-lock) sur l'entrée d'index correspondant à la clé primaire de la séance.
- Les requêtes concurrentes tentant d'exécuter `findByIdWithLock` sur la même séance sont automatiquement suspendues par le gestionnaire de verrous d'InnoDB jusqu'au `COMMIT` ou `ROLLBACK` de la première transaction.
- Cette sérialisation garantit que l'agrégat `sumPlacesReservees` reflète toujours les écritures déjà validées. L'invariant de capacité (`placesReservées + demande <= capacitéSalle`) est ainsi préservé de manière absolue, y compris dans une architecture distribuée où plusieurs instances du backend s'exécuteraient en parallèle (puisque la sérialisation est déléguée au moteur de base de données partagé).

---

## 3. Phase 1 : Initialisation du socle technique

- **Dépôt Git & Wrapper :** Dépôt Git initialisé sur la branche principale `main` avec `.gitignore` complet (exclusion de `target/`, fichiers d'IDE `.idea/`, `.vscode/`, `.env`). Mise en place du Maven Wrapper exécutable sous Windows et Linux (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`).
- **Configuration Maven & Java 25 :** Déclaration du parent `spring-boot-starter-parent` en version `4.1.1`, ciblage du compilateur sur la release 25 (`<maven.compiler.release>25</maven.compiler.release>` et `<java.version>25</java.version>`). Détection automatique du JDK 25 configuré sur la machine.
- **Dépendances intégrées :**
  - Socle & persistance : `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`, `spring-boot-starter-flyway`, `flyway-mysql`, `mysql-connector-j`.
  - Exploitation & documentation : `spring-boot-starter-actuator` (limité à `/health`), `springdoc-openapi-starter-webmvc-ui:3.1.1` (chemins `/v3/api-docs` et `/swagger-ui.html`).
  - Tests : `spring-boot-starter-test`, `spring-boot-starter-security-test`, `spring-boot-testcontainers`, `testcontainers-mysql`, `testcontainers-junit-jupiter`, `spring-boot-starter-webmvc-test`.
  - Respect strict des consignes : aucune dépendance superflue (zéro Lombok, zéro H2).
- **Docker Compose :** Service MySQL 8.4 configuré (`billetterie_db`, `cinema_user` / `cinema_pass`), port 3306 exposé, volume nommé persistant et healthcheck configuré via `mysqladmin ping`.
- **Configuration applicative (`application.yml`) :** Datasource pointant sur MySQL local, `spring.jpa.hibernate.ddl-auto: validate`, `spring.jpa.open-in-view: false`, Actuator configuré pour n'exposer que `health`, et dimensionnement du pool HikariCP (`maximum-pool-size: 20`).
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

## 5. Phase 3 : Conception de l'API REST, Sécurité et Validation de la Recette

### 5.1. Choix d'architecture et découplage par DTOs
- **Immuabilité et Records Java 25 :** Tous les objets de transfert de données (DTOs d'entrée et de sortie) sont modélisés sous forme de `record` Java 25. Cela garantit une immuabilité structurelle, supprime le besoin de boilerplate (getters, constructeurs, `equals`, `hashCode`, `toString`) sans faire appel à un processeur tiers comme Lombok.
- **Étanchéité et non-exposition du modèle JPA :** Aucune entité JPA (`Film`, `Salle`, `Seance`, `Reservation`) n'est exposée directement aux clients de l'API REST. Les DTOs de sortie (`FilmResponse`, `SalleResponse`, `SeanceResponse`, `ReservationResponse`) filtrent strictement les champs retournés, prévenant les fuites d'informations internes, les cycles de sérialisation et les requêtes SQL intempestives hors session transactionnelle (`open-in-view: false`).
- **Validation d'entrée systématique :** Les DTOs de création appliquent les contraintes Jakarta (`@NotBlank`, `@NotNull`, `@Min`, `@Max`, `@Positive`, `@Email`). L'annotation `@Valid` sur les arguments des contrôleurs intercepte les requêtes malformées dès la frontière HTTP.

### 5.2. Gestion centralisée des erreurs et code métier stable
- **Architecture ControllerAdvice :** La classe `GlobalExceptionHandler` annotée `@RestControllerAdvice` intercepte uniformément :
  - `MethodArgumentNotValidException` (retour HTTP 400 avec code `"VALIDATION_ECHOUEE"`).
  - `RessourceIntrouvableException` (retour HTTP 404 avec code `"RESSOURCE_INTROUVABLE"` ou `"SEANCE_INTROUVABLE"`).
  - `CapaciteInsuffisanteException` (retour HTTP 409 avec code `"CAPACITE_INSUFFISANTE"`).
  - `ConflitMetierException` (retour HTTP 409 avec code `"NOM_SALLE_EXISTANT"` ou code de conflit sur mesure).
- **Format normé `ErreurResponse` :** Chaque réponse d'erreur produit un JSON stable (`timestamp`, `status`, `code`, `message`, `path`). Aucune trace d'exécution interne (stack trace) ni détail SQL ou structurel n'est divulgué, respectant les règles d'intégrité et de sécurité applicative.

### 5.3. Sécurité HTTP Basic et gestion des habilitations
- **Stateless & CSRF désactivé :** Configuration Spring Security utilisant `SessionCreationPolicy.STATELESS` et désactivation du CSRF (API REST sans état consommée par des clients programmatiques).
- **Hachage BCrypt & 3 rôles :**
  - Mots de passe stockés avec hachage BCrypt.
  - Définition en mémoire des trois utilisateurs de test : `client` (rôle `CLIENT`), `programmateur` (rôle `PROGRAMMATEUR`), `admin` (rôle `ADMIN`).
- **Matrice des autorisations :**
  - Endpoints publics (`permitAll`) : consultation des films (`GET /api/films`), consultation des séances (`GET /api/seances`), métriques Actuator (`GET /actuator/health`), documentation OpenAPI (`/v3/api-docs/**`, `/swagger-ui/**`).
  - Réservations (`/api/reservations/**`) : réservées aux rôles `CLIENT` et `ADMIN`.
  - Administration des salles et séances (`/api/salles/**`, création de séances) : réservées aux rôles `PROGRAMMATEUR` et `ADMIN`.
  - Création de films (`POST /api/films`) : réservée aux rôles `PROGRAMMATEUR` et `ADMIN`.

---

## 6. Phase 4 : Invariant de capacité, Concurrence multi-threadée et Recette A8

### 6.1. Architecture transactionnelle sous contention
- **Atomicité de la réservation :** Dans `ReservationService.creerReservation`, la pose du verrou `PESSIMISTIC_WRITE` sur la séance garantit qu'aucune autre transaction ne peut réserver simultanément sur la même séance.
- **Révocation immédiate sans écriture partielle :** Si la somme des places réservées cumulée à la nouvelle demande excède la capacité de la salle, l'exception `CapaciteInsuffisanteException` est levée. L'annotation `@Transactional` déclenche un `ROLLBACK` intégral : aucune ligne orpheline n'est insérée dans la table `reservation`.

### 6.2. Matrice globale des recettes de validation (Tests d'intégration)

L'ensemble des tests s'exécute sur des conteneurs MySQL 8.4 réels via Testcontainers. Les recettes fonctionnelles et techniques imposées par le cahier des charges sont intégralement validées :

| Recette | Description | Scénario de test | Statut |
| :--- | :--- | :--- | :--- |
| **A3** | Consultation publique des films | Requête anonyme `GET /api/films` renvoyant 200 avec la liste des films. | **Validé** |
| **A4** | Rejet d'une requête invalide | Requête avec un film de durée invalide (30 min < 40) renvoyant 400 et code `VALIDATION_ECHOUEE`. | **Validé** |
| **A5** | Contrôle d'accès et authentification | Création d'une salle sans auth (401), avec compte `client` (403), et avec compte `admin` (201). | **Validé** |
| **A6** | Réservation nominale | Réservation de 4 places par un client authentifié (201 avec code unique et montant total exact). | **Validé** |
| **A7** | Refus de surréservation (conflit de capacité) | Tentative de dépassement de la capacité restante de la salle renvoyant 409 et code `CAPACITE_INSUFFISANTE`. | **Validé** |
| **A8** | **Concurrence & Invariant de capacité (multi-thread)** | **1. Scénario nominal (5 places restantes, 2 requêtes simultanées de 4 places) : exactement 1 succès (201), 1 rejet (409 `CAPACITE_INSUFFISANTE`), total en base = 19/20 (jamais 23).<br>2. Test de stress (10 places restantes, 10 requêtes simultanées de 2 places) : exactement 5 succès (201), 5 rejets (409 `CAPACITE_INSUFFISANTE`), total final = 20/20 (100 % plein, 0 surréservation).** | **Validé** |
| **A9** | Annulation et libération de capacité | Annulation d'une réservation confirmée (204), libération des places et succès d'une nouvelle réservation. | **Validé** |

**Bilan d'exécution :** 9 tests exécutés avec succès (`.\mvnw.cmd clean verify`), 0 échec, 0 erreur (`BUILD SUCCESS`).
