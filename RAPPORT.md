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

Le projet s'est articulé autour de 6 phases méthodologiques strictement cloisonnées :

1. **Phase 1 : Socle technique, Git & Maven Wrapper** : Initialisation du projet sous Java 25, configuration de `pom.xml` sans duplication de versions, initialisation de Git, configuration de l'arborescence et mise en place du wrapper Maven (`mvnw` / `mvnw.cmd`).
2. **Phase 2 : Schéma relationnel, Migrations Flyway et Entités JPA** : Configuration du dépôt distant GitHub, mise en place des migrations `V1__creation_schema.sql` et `V2__donnees_demonstration.sql`, entités JPA et validation sous Testcontainers avec `ddl-auto: validate`.
3. **Phase 3 : DTOs en records, Services transactionnels, Contrôleurs REST et Sécurité** : Création du journal persistant `MEMOIRE.md`, DTOs immuables (`record`), gestion globale des exceptions (`@RestControllerAdvice`), contrôleurs REST et authentification HTTP Basic stateless.
4. **Phase 4 : Concurrence, Verrouillage pessimiste et Recette A8** : Implémentation du verrou exclusif `SELECT ... FOR UPDATE` via `PESSIMISTIC_WRITE` sur les séances, séquence transactionnelle atomique, stress-test multi-threadé sous Testcontainers validant l'absence totale de surréservation.
5. **Phase 5 : Exploitation, Documentation OpenAPI, Bonus Micrometer & README** : Exposition de la documentation OpenAPI 3.1 et de Swagger UI, sonde Actuator Health publique, compteurs métier Micrometer (`places.reservees`, `places.refusees`) et rédaction du manuel technique.
6. **Phase 6 : Front-End Thymeleaf et Interface Utilisateur Artisanale** : Intégration de `spring-boot-starter-thymeleaf`, création de modèles de vue immuables (`record`), développement du contrôleur web `CinemaWebController`, vues HTML5 ergonomiques au thème cinéma sobre (ardoise sombre et or cinéma) et suite de tests web automatisés.

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

## 5. Phase 6 : Front-End Thymeleaf et Interface Utilisateur Artisanale

### 5.1. Philosophie ergonomique et charte graphique
L'interface utilisateur a été conçue sur mesure pour offrir une expérience soignée, fluide et sobre :
- **Thème sombre cinéma artisanal :** Palette de couleurs contrastée et reposante (fond principal ardoise très sombre `#0b0f17`, conteneurs de cartes `#151d2a`, bordures discrètes `#233044`, touches dorées cinéma `#eab308`, vert succès `#10b981`, rouge alerte `#ef4444`). Rejet catégorique des templates d'IA aux dégradés violets criards et cartes encombrées.
- **Typographie système réactive :** Emploi des polices système modernes garantissant un chargement instantané sans requête réseau externe vers Google Fonts.

### 5.2. Parcours utilisateur et écrans développés
1. **Catalogue des films (`GET /` ou `GET /films`) :** Présentation claire des films à l'affiche avec leurs métadonnées (genre, durée formatée en heures et minutes), liste des séances associées, tarif, salle, disponibilité en direct et bouton de réservation immédiat.
2. **Réservation interactive (`GET /seances/{id}/reserver` et `POST /reserver`) :**
   - Récapitulatif détaillé de la séance avec jauge visuelle de remplissage aux couleurs dynamiques (vert < 60 %, orange 60-90 %, rouge >= 90 %).
   - Formulaire épuré demandant l'email et le nombre de places.
   - Calcul dynamique instantané du montant total via un script JavaScript natif (vanilla JS de 10 lignes sans framework externe).
   - En cas de surréservation, réaffichage de la vue avec une alerte rouge explicite ("Capacité insuffisante : il ne reste que X place(s) disponible(s)") tout en conservant les saisies de l'utilisateur.
3. **Billet numérique stylisé (`GET /confirmation/{code}`) :**
   - Carte billet soignée inspirée des tickets de cinéma avec bordures pointillées et badge officiel `RES-XXXXXXXXXX`.
   - Bouton d'impression directe (`window.print()`).
   - Bouton d'annulation de commande avec confirmation de sécurité.
4. **Annulation en un clic (`POST /annuler/{code}`) :**
   - Libération instantanée des places en base de données avec bascule du statut vers `ANNULEE`.
   - Redirection vers l'accueil avec notification de succès.
5. **Tableau de bord d'exploitation (`GET /admin/tableau-de-bord`) :**
   - Visualisation temps réel des compteurs Micrometer (`places.reservees`, `places.refusees`), du nombre total de films et de séances.
   - Tableau de supervision de l'ensemble des séances avec taux de remplissage, jauges colorées et statut d'ouverture.

---

## 6. Matrice Complète des Recettes de Validation (A1 à A10 + Suite Web)

Toutes les recettes du cahier des charges ainsi que l'interface utilisateur web ont été validées par des tests automatisés exécutés sur conteneur officiel MySQL 8.4 via Testcontainers :

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
| **Web UI** | **Interface Utilisateur Thymeleaf** | 1. Affichage du catalogue public (`GET /` et `/films`).<br>2. Affichage formulaire séance (`GET /seances/{id}/reserver`).<br>3. Réservation nominale avec redirection 3xx (`POST /reserver`).<br>4. Rejet surréservation 200 avec message d'erreur.<br>5. Affichage billet numérique (`GET /confirmation/{code}`).<br>6. Annulation avec redirection 3xx (`POST /annuler/{code}`).<br>7. Tableau de bord d'exploitation (`GET /admin/tableau-de-bord`). | `CinemaWebControllerTest` | **Validé** |

**Bilan d'exécution final :** **20 tests d'intégration exécutés avec succès** (`.\mvnw.cmd clean verify`), 0 échec, 0 erreur (`BUILD SUCCESS`).

---

## 7. Retour d'Expérience sur la Collaboration avec l'IA

### 7.1. Ce qui a fonctionné efficacement :
1. **Le cadrage strict par `.agentrules` :** Définir en amont les interdits absolus (pas de Lombok, code 100 % en français, zéro H2) a totalement supprimé les hallucinations courantes où l'IA propose par défaut du code en anglais ou des dépendances comme H2.
2. **La mémoire persistante (`MEMOIRE.md`) :** En consignant l'état du système et les décisions techniques à chaque fin de phase, l'agent n'a jamais régressé sur les choix d'architecture antérieurs (ex. conservation systématique du verrou pessimiste et des DTOs en records).
3. **Le découpage par phases et les tests de recette :** L'approche "une phase = une branche Git = des tests Testcontainers = un commit" a permis de valider le comportement réel sous MySQL 8.4 au fur et à mesure, rendant les phases finales très sereines.

### 7.2. Pièges évités :
1. **La modularité de Spring Boot 4 :** Dans Spring Boot 4, certaines dépendances et annotations de test ont été restructurées (ex. `AutoConfigureMockMvc` déplacé dans `spring-boot-starter-webmvc-test`). L'utilisation du client HTTP natif Java 25 (`java.net.http.HttpClient`) a permis de s'affranchir de toute dépendance fragile tout en exploitant pleinement les capacités modernes du JDK 25.
2. **Le verrouillage sous concurrence :** L'IA aurait pu être tentée d'utiliser une synchronisation Java en mémoire (`synchronized`). Le cadrage a imposé la délégation de la concurrence à InnoDB via `SELECT ... FOR UPDATE`, garantissant l'intégrité même en contexte distribué.
3. **Les pièges de parsing Thymeleaf :** Lors de l'écriture des fragments d'en-tête, l'utilisation de simples guillemets au sein d'expressions Thymeleaf a provoqué une erreur de parsing. La détection immédiate lors de l'exécution du test `CinemaWebControllerTest` a permis de corriger la syntaxe immédiatement.
