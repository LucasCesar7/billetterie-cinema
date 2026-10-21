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
  - `Film` (id, titre, genre, dureeMinutes, dateSortie)
  - `Salle` (id, nom, capacite)
  - `Seance` (id, film, salle, debut, prixUnitaire en `BigDecimal`)
  - `Reservation` (id, code, seance, emailClient, nombrePlaces, statut `StatutReservation`, creeeLe)
  - `StatutReservation` : enum (`CONFIRMEE`, `ANNULEE`).
- **Repositories Spring Data (`fr.univ.billetterie.repository`) :**
  - `FilmRepository` : filtres de recherche.
  - `SalleRepository` : recherche et vérification par nom.
  - `SeanceRepository` : recherche par créneau et méthode verrouillée `findByIdWithLock(Long id)` avec `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
  - `ReservationRepository` : recherche par code et requête agrégée `sumPlacesReservees`.
- **Validation Testcontainers :**
  - `SchemaValidationTest` passe avec succès sur un conteneur MySQL 8.4 réel (0 échec, validation Hibernate réussie).

---

## 3. Phase 3 en cours : API REST, DTOs, Gestion des erreurs et Sécurité HTTP Basic

- Branche courante : `feature/phase3-api-rest`.
- Objectifs :
  1. DTOs d'entrées et de sorties sous forme de `record` Java 25 (`fr.univ.billetterie.dto`).
  2. Exceptions métier (`RessourceIntrouvableException`, `CapaciteInsuffisanteException`, `ConflitMetierException`) dans `fr.univ.billetterie.exception`.
  3. Gestionnaire global d'exceptions `GlobalExceptionHandler` avec format `ErreurResponse` normé.
  4. Services métier transactionnels (`FilmService`, `SalleService`, `SeanceService`, `ReservationService`).
  5. Configuration Spring Security stateless avec HTTP Basic et 3 rôles (`CLIENT`, `PROGRAMMATEUR`, `ADMIN`).
  6. Contrôleurs REST (`FilmController`, `SalleController`, `SeanceController`, `ReservationController`).
  7. Tests automatisés couvrant les contrôles de recette A3, A4, A5, A6, A7, A9.
  8. Merge sur `main` et push vers GitHub.

