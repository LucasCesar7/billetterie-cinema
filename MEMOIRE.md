# Mémoire du projet - Backend Billetterie Cinéma

Ce document consigne l'état courant du projet, les choix d'architecture validés, l'historique complet des phases et les règles strictes garantissant l'intégrité de l'application et l'absence totale de régression.

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

## 2. Historique et État Final du Système

### Phase 1 (Terminée & Validée) - Recette A1
- Wrapper Maven 3.3.4 configuré avec détection automatique du JDK 25 local (`loom-ea-25-loom+1-11`).
- Dépendances Spring Boot 4.1.1 intégrées sans versions explicites redondantes.
- Docker Compose avec service `mysql:8.4` (healthcheck, base `billetterie_db`, utilisateur `cinema_user`).
- Fichier `application.yml` configuré (`ddl-auto: validate`, `open-in-view: false`, Actuator restreint à `/health`, doc OpenAPI sur `/v3/api-docs` et `/swagger-ui.html`).
- Dépôt Git relié au dépôt distant `https://github.com/LucasCesar7/billetterie-cinema.git`.

### Phase 2 (Terminée & Validée) - Recette A2
- **Migrations Flyway :**
  - `V1__creation_schema.sql` : Tables `film`, `salle`, `seance`, `reservation` avec clés primaires `BIGINT`, clés étrangères `ON DELETE RESTRICT`, contraintes `CHECK`, contraintes d'unicité sur `salle.nom` et `reservation.code`, et index optimisés.
  - `V2__donnees_demonstration.sql` : Données de démonstration (*Inception*, *Interstellar*, *Salle 1* de 100 places, séance future à 10.50 €).
- **Entités JPA (`fr.univ.billetterie.entity`) :**
  - `Film`, `Salle`, `Seance`, `Reservation`, `StatutReservation` (enum).
- **Repositories Spring Data (`fr.univ.billetterie.repository`) :**
  - `FilmRepository`, `SalleRepository`, `SeanceRepository` (avec `findByIdWithLock`), `ReservationRepository` (avec `sumPlacesReservees`).
- **Validation Testcontainers :**
  - `SchemaValidationTest` validé avec succès (validation Hibernate et requêtes JPA).

### Phase 3 (Terminée & Validée) - Recettes A3, A4, A5, A6, A7, A9
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

### Phase 4 (Terminée & Validée) - Recette A8
- **Invariant de capacité et verrouillage pessimiste :**
  - Verrou pessimiste en écriture `@Lock(LockModeType.PESSIMISTIC_WRITE)` sur `SeanceRepository.findByIdWithLock(id)`.
  - Hibernate émet `SELECT ... FOR UPDATE` sur la ligne de la séance ciblée sous le moteur MySQL InnoDB.
  - Séquence transactionnelle atomique dans `ReservationService.reserver(...)` : acquisition du verrou exclusif sur la séance, calcul des places réservées (`sumPlacesReservees`), rejet strict par `CapaciteInsuffisanteException` (409) si dépassement de la capacité de la salle, et rollback immédiat sans effet de bord.
  - Configuration de la datasource avec `spring.datasource.hikari.maximum-pool-size: 20` pour absorber les pics de charge concurrentiels.
- **Validation automatisée de la Recette A8 (`ReservationConcurrenceIntegrationTest`) :**
  - Test 1 (Scénario nominal) : 5 places restantes, 2 requêtes simultanées de 4 places -> 1 succès 201, 1 rejet 409, 4 places attribuées (19/20, jamais 23).
  - Test 2 (Stress test) : 10 places restantes, 10 requêtes simultanées de 2 places -> 5 succès 201, 5 rejets 409, total en base 20/20 (capacité respectée à 100%, 0 surréservation).

### Phase 5 (Terminée & Validée) - Recettes A1, A2, A10 & Bonus Micrometer
- **Exploitation & Documentation OpenAPI 3.1.1 :**
  - `OpenApiConfig` configuré avec métadonnées techniques et schéma de sécurité HTTP Basic (`AuthentificationBasic`).
  - Endpoints Swagger UI (`/swagger-ui.html`, `/swagger-ui/**`) et OpenAPI (`/v3/api-docs`) vérifiés publiquement accessibles sans authentification.
  - Endpoint Actuator (`/actuator/health`) public retournant `{"status":"UP"}`. Les autres endpoints sensibles demeurent non exposés.
- **Bonus Métier Micrometer :**
  - Compteurs métier intégrés dans `ReservationService` via `MeterRegistry` :
    * `billetterie.places.reservees` : incrémenté lors d'une réservation confirmée.
    * `billetterie.places.refusees` : incrémenté lors d'un refus pour capacité insuffisante.
- **Tests d'exploitation (`ExploitationIntegrationTest`) :**
  - Validation de `/actuator/health` (200 UP), `/v3/api-docs` (200), `/swagger-ui.html` (200/302), et incrémentation effective des compteurs Micrometer.
- **Livrable & Documentation :**
  - `README.md` complet rédigé à la racine avec prérequis, commandes de référence, vision UX, tableau des endpoints et comptes de démo.
  - `RAPPORT.md` finalisé avec matrice intégrale A1 à A10 et retour d'expérience.

### Phase 6 (Terminée & Validée) - Interface Utilisateur Web Thymeleaf
- **Dépendance Maven & Intégration :**
  - `spring-boot-starter-thymeleaf` ajouté dans `pom.xml` géré par le parent Spring Boot 4.1.1.
  - Propriétés d'encodage `UTF-8` explicites dans `pom.xml`.
- **Sécurité Web (`SecurityConfig`) :**
  - Accès public sans authentification configuré pour les routes de consultation et d'action du front-end (`/`, `/films`, `/seances/**`, `/reserver/**`, `/confirmation/**`, `/annuler/**`, `/admin/tableau-de-bord`, `/css/**`, `/js/**`, `/images/**`, `/favicon.ico`).
  - Préservation stricte et intégrale de la sécurité HTTP Basic sur l'API REST (`/api/...`).
- **Modèles de Vue Immuables (`fr.univ.billetterie.dto.web`) :**
  - `FilmCatalogueVue`, `SeanceResumeVue`, `SeanceDetailVue`, `BilletConfirmationVue`, `TableauDeBordVue` (records Java natifs, zéro Lombok, prévention absolue des `LazyInitializationException` avec `JOIN FETCH`).
- **Contrôleur Web (`fr.univ.billetterie.controller.web.CinemaWebController`) :**
  - `GET /` et `GET /films` : affichage du catalogue avec horaires, salles, tarifs et badges d'état.
  - `GET /seances/{id}/reserver` : formulaire de réservation avec jauge visuelle et calcul dynamique du montant en vanilla JS.
  - `POST /reserver` : réservation transactionnelle avec redirection `/confirmation/{code}` ou réaffichage élégant avec message d'alerte en cas de surréservation.
  - `GET /confirmation/{code}` : billet numérique stylisé avec détails complets, bouton d'impression et annulation.
  - `POST /annuler/{code}` : annulation immédiate avec libération des places et redirection accueil.
  - `GET /admin/tableau-de-bord` : supervision en direct des jauges d'occupation de toutes les séances et des métriques Micrometer (`places.reservees` et `places.refusees`).
- **Charte Graphique et Vues HTML5 :**
  - Design cinéma artisanal sobre et moderne (fond `#0b0f17`, cartes `#151d2a`, bordures `#233044`, or `#eab308`, succès `#10b981`, danger `#ef4444`).
  - Templates dans `src/main/resources/templates/` (`layout.html`, `index.html`, `reservation.html`, `confirmation.html`, `tableau-de-bord.html`) et CSS `src/main/resources/static/css/style.css`.
- **Validation Automatisée (`CinemaWebControllerTest`) :**
  - 7 tests web validés (affichage catalogue, affichage réservation, réservation nominale 3xx avec redirection, refus surréservation 200 avec alerte, affichage billet, annulation 3xx avec libération, tableau de bord admin).
- **Bilan d'exécution final :**
  - 20 tests d'intégration au vert sur MySQL 8.4 via Testcontainers, 0 échec, 0 erreur (`BUILD SUCCESS`).
  - JAR autonome `billetterie-cinema-0.0.1-SNAPSHOT.jar` repackagé avec succès.
