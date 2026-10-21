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

## 2. Démarche Méthodologique et Cycle de Développement

Le projet s'est articulé autour de 7 phases méthodologiques strictement cloisonnées :

1. **Phase 1 : Socle technique, Git & Maven Wrapper** : Initialisation du projet sous Java 25, configuration de `pom.xml` sans duplication de versions, initialisation de Git, configuration de l'arborescence et mise en place du wrapper Maven (`mvnw` / `mvnw.cmd`).
2. **Phase 2 : Schéma relationnel, Migrations Flyway et Entités JPA** : Configuration du dépôt distant GitHub, mise en place des migrations `V1__creation_schema.sql` et `V2__donnees_demonstration.sql`, entités JPA et validation sous Testcontainers avec `ddl-auto: validate`.
3. **Phase 3 : DTOs en records, Services transactionnels, Contrôleurs REST et Sécurité** : Création du journal persistant `MEMOIRE.md`, DTOs immuables (`record`), gestion globale des exceptions (`@RestControllerAdvice`), contrôleurs REST et authentification HTTP Basic stateless.
4. **Phase 4 : Concurrence, Verrouillage pessimiste et Recette A8** : Implémentation du verrou exclusif `SELECT ... FOR UPDATE` via `PESSIMISTIC_WRITE` sur les séances, séquence transactionnelle atomique, stress-test multi-threadé sous Testcontainers validant l'absence totale de surréservation.
5. **Phase 5 : Exploitation, Documentation OpenAPI, Bonus Micrometer & README** : Exposition de la documentation OpenAPI 3.1 et de Swagger UI, sonde Actuator Health publique, compteurs métier Micrometer (`places.reservees`, `places.refusees`) et rédaction du manuel technique.
6. **Phase 6 : Front-End Thymeleaf et Interface Utilisateur Artisanale** : Intégration de `spring-boot-starter-thymeleaf`, création de modèles de vue immuables (`record`), développement du contrôleur web `CinemaWebController`, vues HTML5 ergonomiques au thème cinéma sobre (ardoise sombre et or cinéma) et suite de tests web automatisés.
7. **Phase 7 : Authentification Web (Form Login), Sessions & Écrans de Gestion** : Architecture double `SecurityFilterChain` (`apiSecurityFilterChain` stateless avec HTTP Basic et 401 strict ; `webSecurityFilterChain` avec Form Login, sessions et redirections 302), boutons de test rapide 1-clic pour tous les rôles, et formulaires web d'administration et de programmation.

---

## 3. Choix d'Architecture Logicielle

### 3.1. Approche en couches étanches et records Java 25
Le backend applique une séparation stricte des responsabilités :
- **Couche Présentation (REST & Web) :** Réception des requêtes HTTP, validation syntaxique Jakarta (`@Valid`), délégation immédiate à la couche service et conversion vers des modèles de vue ou DTOs immuables.
- **Couche Métier (Service) :** Frontière transactionnelle (`@Transactional`), délimitation des règles de gestion, application des verrous d'isolation et incrémentation des compteurs d'exploitation.
- **Couche Persistance (Repository & JPA) :** Accès aux données relationnelles via Spring Data JPA avec requêtes optimisées `JOIN FETCH` pour supprimer les risques de requêtes N+1 et de `LazyInitializationException` en dehors de la session Hibernate (`open-in-view: false`).

### 3.2. Remplacement intégral de Lombok par Java 25 natif
Conformément aux directives de `.agentrules`, aucune dépendance à Lombok n'a été introduite :
- Les DTOs d'entrée, de sortie et les modèles de vue pour Thymeleaf sont déclarés sous la forme de `record` Java natifs, garantissant l'immuabilité des structures de données, la génération automatique des constructeurs compacts, des accesseurs, de `equals`, `hashCode` et `toString`.
- Les entités JPA (`Film`, `Salle`, `Seance`, `Reservation`) conservent des constructeurs explicites et des accesseurs standards, assurant une parfaite compatibilité avec les spécifications Jakarta Persistence sans aucune magie de bytecode.

---

## 4. Stratégie de Persistance et Gestion de la Concurrence

### 4.1. Schéma relationnel strict et validation Flyway
Le schéma SQL a été entièrement conçu pour garantir l'intégrité référentielle et relationnelle directement au niveau de la base MySQL :
- Clés primaires `BIGINT AUTO_INCREMENT` et clés étrangères protégées par `ON DELETE RESTRICT` (interdisant la suppression accidentelle d'une salle ou d'un film ayant des séances rattachées).
- Contraintes d'intégrité déclaratives `CHECK` sur les durées, capacités, prix unitaires et nombres de places.
- Contraintes d'unicité sur le nom des salles physiques (`salle.nom`) et sur le code officiel de réservation (`reservation.code`).
- La configuration Hibernate `spring.jpa.hibernate.ddl-auto: validate` impose une synchronisation rigoureuse entre les scripts Flyway et les métadonnées JPA.

### 4.2. Invariant de capacité sous contention (Recette A8)
Pour éliminer le risque de surréservation lors de requêtes concurrentes :
1. **Échec de l'approche optimiste (`@Version`) :** Sous forte charge, un verrouillage optimiste rejetterait massivement les réservations avec des erreurs `OptimisticLockException`, imposant une boucle de retry complexe et coûteuse pour les clients.
2. **Adoption du verrouillage pessimiste (`PESSIMISTIC_WRITE`) :** L'appel `seanceRepository.findByIdWithLock(seanceId)` génère la clause SQL `SELECT ... FOR UPDATE` sur la ligne ciblée de la table `seance` sous le moteur de stockage MySQL InnoDB.
3. **Atomicité garantie :** Les transactions concurrentes sont ordonnancées de manière déterministe. Chaque transaction calcule la somme exacte des places déjà confirmées (`sumPlacesReservees`). Si la capacité de la salle est insuffisante, l'exception `CapaciteInsuffisanteException` est immédiatement levée, déclenchant un `ROLLBACK` intégral sans aucune écriture partielle en base.

---

## 5. Architecture de Sécurité : Double SecurityFilterChain

Afin de combiner harmonieusement une API REST consommée de façon programmatique et une application web interactive avec interface utilisateur dans le navigateur :

1. **Chaîne API REST (`@Order(1)`) :**
   - Cible exclusivement `/api/**` (`.securityMatcher("/api/**")`).
   - Mode sans état strict (`SessionCreationPolicy.STATELESS`), CSRF désactivé, authentification HTTP Basic activée.
   - En cas d'appel non authentifié, le point d'entrée renvoie un strict code `401 Unauthorized` (sans aucune redirection 302 vers une page de login).
2. **Chaîne Web UI (`@Order(2)`) :**
   - Cible l'ensemble des routes web (`/**`).
   - Active le Form Login (`/connexion`) et la déconnexion (`/deconnexion`).
   - Redirige automatiquement tout visiteur non authentifié (302) vers `/connexion` lors d'une tentative d'accès aux routes protégées (`/admin/**`, `/programmation/**`).
   - Intègre les tags de sécurité Thymeleaf (`sec:authorize`) pour adapter dynamiquement la barre de navigation selon le profil connecté.

---

## 6. Matrice Complète des Recettes de Validation (A1 à A10 + Suites Web & Auth)

Toutes les recettes du cahier des charges ainsi que l'interface utilisateur web et la sécurité ont été validées par des tests automatisés exécutés sur conteneur officiel MySQL 8.4 via Testcontainers :

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
| **Web UI & Auth** | **Interface Utilisateur, Form Login & Gestion** | 1. Affichage du catalogue public (`GET /` et `/films`).<br>2. Affichage page de connexion (`GET /connexion`).<br>3. Affichage formulaire séance (`GET /seances/{id}/reserver`).<br>4. Réservation nominale avec redirection 3xx (`POST /reserver`).<br>5. Rejet surréservation 200 avec message d'alerte.<br>6. Affichage billet numérique (`GET /confirmation/{code}`).<br>7. Annulation avec redirection 3xx (`POST /annuler/{code}`).<br>8. Redirection 3xx vers `/connexion` pour anonymes sur `/admin/tableau-de-bord`.<br>9. Accès 200 au tableau de bord pour `ADMIN`.<br>10. Accès 200 à la gestion de programmation pour `PROGRAMMATEUR`.<br>11. Accès 200 à la gestion des salles pour `ADMIN`. | `CinemaWebControllerTest` | **Validé** |

**Bilan d'exécution final :** **24 tests d'intégration exécutés avec succès** (`.\mvnw.cmd clean verify`), 0 échec, 0 erreur (`BUILD SUCCESS`).

---

## 7. Retour d'Expérience sur la Collaboration avec l'IA

### 7.1. Ce qui a fonctionné efficacement :
1. **Le cadrage strict par `.agentrules` :** Définir en amont les interdits absolus (pas de Lombok, code 100 % en français, zéro H2) a totalement supprimé les hallucinations courantes où l'IA propose par défaut du code en anglais ou des dépendances comme H2.
2. **La mémoire persistante (`MEMOIRE.md`) :** En consignant l'état du système et les décisions techniques à chaque fin de phase, l'agent n'a jamais régressé sur les choix d'architecture antérieurs (ex. conservation systématique du verrou pessimiste, des DTOs en records et des filtres de sécurité).
3. **Le découpage par phases et les tests de recette :** L'approche "une phase = une branche Git = des tests Testcontainers = un commit" a permis de valider le comportement réel sous MySQL 8.4 au fur et à mesure, rendant les phases d'extension rapides et sans risque.

### 7.2. Pièges évités :
1. **La séparation des chaînes de sécurité :** Éviter l'écueil classique où l'ajout d'une page de login web casse les tests REST existants en renvoyant des redirections HTTP 302 au lieu de codes `401 Unauthorized`. L'utilisation ordonnée de deux chaînes (`@Order(1)` et `@Order(2)`) a permis d'isoler parfaitement les deux contextes d'exécution.
2. **Le verrouillage sous concurrence :** L'IA aurait pu être tentée d'utiliser une synchronisation Java en mémoire (`synchronized`). Le cadrage a imposé la délégation de la concurrence à InnoDB via `SELECT ... FOR UPDATE`, garantissant l'intégrité même en contexte distribué.
3. **Les pièges de parsing Thymeleaf :** Détection et résolution immédiate des subtilités de syntaxe de fragments Thymeleaf et intégration directe des expressions de sécurité `sec:authorize`.
