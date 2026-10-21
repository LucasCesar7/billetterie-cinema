# Mémoire du projet - Backend Billetterie Cinéma

Ce document consigne l'état courant du projet, les choix d'architecture validés, l'historique des phases et les règles strictes à observer pour préserver l'intégrité de l'application et éviter toute régression.

---

## 1. Règles d'or du projet (.agentrules)

- **Langue obligatoire :** 100 % en français (code, noms de classes, méthodes, attributs, tables, colonnes, messages d'erreurs, documentation).
- **Zéro Lombok :** Exploitation des fonctionnalités natives de Java 25. Les DTOs et corps de réponses sont des `record` immuables. Les entités JPA utilisent des constructeurs et getters/setters standards.
- **Base de données MySQL stricte :** Interdiction d'ajouter H2. Tous les tests d'intégration s'exécutent sur MySQL 8.4 via Testcontainers.
- **Validation Flyway stricte :** `spring.jpa.hibernate.ddl-auto: validate`. Toute modification structurelle passe exclusivement par un script de migration Flyway versionné (`V1`, `V2`, etc.).
- **Architecture en couches étanches :**
  - Contrôleurs fins, validation Jakarta en amont (`@Valid`).
  - DTOs d'entrée et de sortie exclusifs (ne jamais exposer d'entités JPA directement).
  - Services transactionnels délimitant les règles métier et appliquant les verrous.
  - Exceptions métier spécifiques traduites par un `@RestControllerAdvice` centralisé (sans fuite de détails techniques ou SQL).
  - Gestion de la concurrence par verrouillage pessimiste en base (`SELECT ... FOR UPDATE`).
- **Autonomie Git :** Validation par build/tests Maven avant chaque commit conventionnel, travail sur branche de fonctionnalité puis merge sur `main` et push sur GitHub.

---

## 2. État courant du système

### Phase 1 (Terminée & Validée)
- Wrapper Maven 3.3.4 configuré avec détection automatique du JDK 25 local (`loom-ea-25-loom+1-11`).
- Dépendances Spring Boot 4.1.1 intégrées sans versions explicites redondantes.
- Docker Compose avec service `mysql:8.4` (healthcheck, base `billetterie_db`, utilisateur `cinema_user`).
- Fichier `application.yml` configuré (`ddl-auto: validate`, `open-in-view: false`, Actuator restreint à `/health`, doc OpenAPI sur `/v3/api-docs` et `/swagger-ui.html`).
- Dépôt Git relié au dépôt distant `https://github.com/LucasCesar7/billetterie-cinema.git`.

### Phase 2 (Terminée & Validée)
- **Migrations Flyway :**
  - `V1__creation_schema.sql` : Tables `film`, `salle`, `seance`, `reservation` avec clés primaires `BIGINT`, clés étrangères `ON DELETE RESTRICT`, contraintes `CHECK`, contraintes d'unicité sur `salle.nom` et `reservation.code`, et index optimisés.
  - `V2__donnees_demonstration.sql` : Données de démonstration (*Inception*, *Interstellar*, *Salle 1* de 100 places, séance future à 10.50 €).
- **Entités JPA (`fr.univ.billetterie.entity`) :**
  - `Film`, `Salle`, `Seance`, `Reservation`, `StatutReservation` (enum).
- **Repositories Spring Data (`fr.univ.billetterie.repository`) :**
  - `FilmRepository`, `SalleRepository`, `SeanceRepository` (avec `findByIdWithLock`), `ReservationRepository` (avec `sumPlacesReservees`).
- **Validation Testcontainers :**
  - `SchemaValidationTest` validé avec succès (validation Hibernate et requêtes JPA).

### Phase 3 (Terminée & Validée)
- **DTOs immuables (`fr.univ.billetterie.dto`) :**
  - Requêtes : `FilmCreationRequest`, `SalleCreationRequest`, `SeanceCreationRequest`, `ReservationCreationRequest` avec annotations Jakarta (`@NotBlank`, `@Min`, `@Max`, `@Positive`, `@Email`).
  - Réponses : `FilmResponse`, `SalleResponse`, `SeanceResponse`, `ReservationResponse`, `ErreurResponse`.
- **Exceptions métier & Gestionnaire global (`fr.univ.billetterie.exception` & `controller`) :**
  - `RessourceIntrouvableException` (404), `CapaciteInsuffisanteException` (409), `ConflitMetierException` (409).
  - `GlobalExceptionHandler` annoté `@RestControllerAdvice` formatant les erreurs en JSON stable sans trace de pile.
- **Couche service transactionnelle (`fr.univ.billetterie.service`) :**
  - `FilmService`, `SalleService` (contrôle d'unicité), `SeanceService` (contrôle d'existence), `ReservationService` (verrouillage pessimiste, contrôle de capacité, calcul du montant total, annulation).
- **Sécurité HTTP Basic stateless (`fr.univ.billetterie.config.SecurityConfig`) :**
  - Mots de passe hachés BCrypt, 3 comptes en mémoire (`client`, `programmateur`, `admin`).
  - Autorisations fines par rôles et endpoints publics pour consultation et santé.
- **Contrôleurs REST fins (`fr.univ.billetterie.controller`) :**
  - `FilmController`, `SalleController`, `SeanceController`, `ReservationController` avec codes HTTP appropriés (200, 201 avec header Location, 204).
- **Tests d'intégration (`BilletterieIntegrationTest`) :**
  - Couverture des recettes : A3 (consultation publique), A4 (validation 400), A5 (sécurité 401/403/201), A6 & A7 (réservation 201 et saturation 409), A9 (annulation 204 et libération de places).

### Phase 4 (Terminée & Validée)
- **Invariant de capacité et verrouillage pessimiste :**
  - Verrou pessimiste en écriture `@Lock(LockModeType.PESSIMISTIC_WRITE)` sur `SeanceRepository.findByIdWithLock(id)`.
  - Hibernate émet `SELECT ... FOR UPDATE` sur la ligne de la séance ciblée sous le moteur MySQL InnoDB.
  - Séquence transactionnelle atomique dans `ReservationService.reserver(...)` : acquisition du verrou exclusif sur la séance, calcul des places réservées (`sumPlacesReservees`), rejet strict par `CapaciteInsuffisanteException` (409) si dépassement de la capacité de la salle, et rollback immédiat sans effet de bord.
  - Configuration de la datasource avec `spring.datasource.hikari.maximum-pool-size: 20` pour absorber les pics de charge concurrentiels.
- **Validation automatisée de la Recette A8 (`ReservationConcurrenceIntegrationTest`) :**
  - Test 1 (Scénario nominal du sujet) : Séance avec 5 places disponibles restantes, 2 requêtes concurrentes déclenchées au même instant (`CountDownLatch`) demandant 4 places chacune. Résultat : exactement 1 succès (201), 1 rejet (409 `CAPACITE_INSUFFISANTE`), et exactement 4 places attribuées en base (total 19/20, jamais 23).
  - Test 2 (Test de stress) : Séance avec 10 places restantes, 10 requêtes concurrentes déclenchées au même instant demandant 2 places chacune. Résultat : exactement 5 succès (201), 5 rejets (409 `CAPACITE_INSUFFISANTE`), total en base égal à 20/20 places (capacité maximale respectée à 100%, 0 surréservation).
- **Bilan d'exécution Maven :**
  - 9 tests d'intégration exécutés sur conteneur MySQL 8.4 réel (`SchemaValidationTest`, `BilletterieIntegrationTest`, `ReservationConcurrenceIntegrationTest`).
  - 0 échec, 0 erreur (`BUILD SUCCESS`).
