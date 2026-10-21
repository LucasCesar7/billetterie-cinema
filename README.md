# Backend Billetterie Cinéma - Haute Fiabilité (R5.08)

Backend de billetterie de cinéma développé en **Java 25**, **Spring Boot 4.1.1**, **MySQL 8.4**, **Flyway** et **Thymeleaf**, conçu pour garantir un invariant strict de capacité même sous forte contention multi-threadée et fournir un CRUD complet pour la gestion des films, des salles physiques et des séances.

---

## 1. Présentation & Philosophie d'Ingénierie

### Contexte technique
Ce projet implémente le système complet de billetterie d'un cinéma :
- **Modèle relationnel strict :** Intégrité référentielle en base MySQL, validation Flyway et Hibernate `ddl-auto: validate`. Clés étrangères avec `ON DELETE RESTRICT`.
- **Architecture en couches étanches :** Séparation nette entre contrôleurs REST/Web, services transactionnels et persistance.
- **Java 25 natif & Zéro Lombok :** Exploitation intégrale des `record` immuables pour tous les DTOs et modèles de vue.
- **Double SecurityFilterChain :**
  - Chaîne API REST stateless (`/api/**`) avec HTTP Basic (rejet 401 strict sans redirection).
  - Chaîne Web UI (`/**`) avec Form Login, gestion de sessions, boutons de test 1-clic et redirections 302.
- **Verrouillage pessimiste InnoDB (`PESSIMISTIC_WRITE`) :** Ordonnancement séquentiel via `SELECT ... FOR UPDATE` pour empêcher toute surréservation sans mécanisme de retry côté client.
- **Interface Web Artisanale (Thymeleaf) :** Vues HTML5 responsives, sobres et contrastées au thème cinéma ardoise et or, avec opérations CRUD complètes et dialogues de confirmation de suppression.

### Méthodologie de développement assisté par IA
Le développement a été réalisé en binôme avec l'agent autonome Antigravity (piloté par Gemini 3.8 Flash) sous VS Code, selon un cadre d'ingénierie rigoureux :
- **Cadrage humain préalable :** Rédaction d'un fichier de règles strictes ([`.agentrules`](.agentrules)) imposant du code 100 % en français, zéro processeur d'annotations tiers (zéro Lombok), l'exclusion totale de bases simulées comme H2, et l'usage exclusif de conteneurs MySQL 8.4 via Testcontainers.
- **Découpage modulaire par phases :** Progression incrémentale par branches Git dédiées (Phases 1 à 8).
- **Zéro régression :** Chaque phase a fait l'objet d'une compilation du bytecode, d'une exécution de tests automatisés sous conteneurs réels et d'une consignation dans un journal persistant ([`MEMOIRE.md`](MEMOIRE.md)).

---

## 2. Interface Utilisateur Web (Thymeleaf)

L'application intègre un front-end complet développé avec le moteur de template **Thymeleaf**, offrant une expérience utilisateur sobre, fluide et accessible sans dépendance à des frameworks JavaScript lourds :

- **Design sombre cinéma artisanal :** Fond ardoise très sombre (`#0b0f17`), conteneurs de cartes (`#151d2a`), bordures discrètes (`#233044`) et accents dorés cinéma (`#eab308`).
- **Catalogue des films & séances :** Accessible sur `http://localhost:8080/` ou `http://localhost:8080/films`. Affiche les affiches, genres, durées formatées, salles, horaires, prix et disponibilités en direct.
- **Formulaire de réservation interactif :** Accessible depuis chaque séance via `http://localhost:8080/seances/{id}/reserver`. Intègre une jauge d'occupation en temps réel et un calcul instantané du montant total en JavaScript natif. En cas de surréservation, la vue est réaffichée immédiatement avec une alerte rouge explicite.
- **Billet numérique stylisé :** Accessible sur `http://localhost:8080/confirmation/{code}`. Billet format ticket de cinéma avec récapitulatif complet, code officiel unique (`RES-XXXXXXXXXX`), bouton d'impression directe (`window.print()`) et formulaire d'annulation sécurisé.
- **Page de connexion & boutons 1-clic :** Accessible sur `http://localhost:8080/connexion`. Permet de se connecter classiquement ou d'utiliser les 3 boutons de test rapide pour basculer instantanément entre les rôles `Client`, `Programmateur` et `Admin`.
- **Écrans d'administration & programmation (CRUD complet) :**
  - `http://localhost:8080/programmation` : ajout, modification et suppression de films et de séances, avec vérification des séances ou réservations rattachées.
  - `http://localhost:8080/admin/salles` : déclaration, renommage, mise à jour de capacité et suppression sécurisée des salles physiques.
  - `http://localhost:8080/admin/tableau-de-bord` : supervision en direct des jauges d'occupation et des métriques Micrometer.

---

## 3. Prérequis Système

- **Java Development Kit (JDK) 25** avec la variable d'environnement `JAVA_HOME` configurée.
- **Docker & Docker Compose** (moteur Docker en cours d'exécution) pour l'exécution du service MySQL 8.4 local et des conteneurs éphémères Testcontainers.
- Accès réseau sortant pour le téléchargement initial des dépendances Maven et des images Docker officielles.

---

## 4. Démarrage de l'Environnement

### Base de données locale (Docker Compose)
Pour lancer l'instance MySQL 8.4 dédiée au développement local :
```bash
docker compose up -d
```

Pour arrêter le service et libérer les ressources :
```bash
docker compose down
```

*Note : La base est configurée avec l'utilisateur `cinema_user` / `cinema_pass` sur la base `billetterie_db` exposée sur le port 3306.*

---

## 5. Commandes de Référence (Maven Wrapper)

Le projet intègre le wrapper Maven (`mvnw` / `mvnw.cmd`), garantissant une reproductibilité exacte du build sans installation locale de Maven.

### Exécution de la suite de tests et vérification globale
Lance l'ensemble de la suite de tests d'intégration (schéma, CRUD REST, sécurité, concurrence multi-threadée, exploitation et contrôleur web Thymeleaf avec sécurité des sessions) sous conteneurs MySQL 8.4 réels :

Sous Windows (PowerShell / CMD) :
```powershell
.\mvnw.cmd clean verify
```

Sous Linux / macOS :
```bash
./mvnw clean verify
```

### Lancement de l'application hors IDE
Une fois la commande `clean verify` ou `clean package` exécutée, l'archive exécutable autonome est disponible dans le dossier `target/` :
```bash
java -jar target/billetterie-cinema-0.0.1-SNAPSHOT.jar
```
L'application démarre par défaut sur le port HTTP `8080`.

---

## 6. Comptes de Démonstration (Authentification)

L'application gère trois comptes utilisateurs avec mots de passe hachés via BCrypt :

| Identifiant | Mot de passe | Rôle Spring Security | Périmètre d'action Web & REST |
| :--- | :--- | :--- | :--- |
| `client` | `client123` | `ROLE_CLIENT` | Effectuer une réservation, annuler son billet. |
| `programmateur` | `prog123` | `ROLE_PROGRAMMATEUR` | Créer, modifier et supprimer des films et séances (`/programmation`). |
| `admin` | `admin123` | `ROLE_ADMIN` | Gérer les salles (`/admin/salles`), tableau de bord superviseur (`/admin/tableau-de-bord`), accès complet. |

---

## 7. Tableau Récapitulatif des Endpoints

### 7.1. Interface Web Thymeleaf (HTML5 / Navigation navigateur)
| Méthode | URL | Accès / Rôle | Rôle fonctionnel |
| :--- | :--- | :--- | :--- |
| `GET` | `/` ou `/films` | Public (Anonyme) | Catalogue public des films et séances programmées |
| `GET` | `/connexion` | Public (Anonyme) | Formulaire d'authentification et boutons de test rapide 1-clic |
| `POST` | `/connexion` | Public (Anonyme) | Traitement de la connexion utilisateur Spring Security |
| `POST` | `/deconnexion` | Authentifié | Déconnexion et invalidation de session |
| `GET` | `/seances/{id}/reserver` | Public (Anonyme) | Page de réservation d'une séance avec jauge et calcul live |
| `POST` | `/reserver` | Public (Anonyme) | Soumission du formulaire de réservation (redirection ou erreur) |
| `GET` | `/confirmation/{code}` | Public (Anonyme) | Vue du billet numérique et option d'annulation |
| `POST` | `/annuler/{code}` | Public (Anonyme) | Annulation de la réservation et libération des places |
| `GET` | `/programmation` | `PROGRAMMATEUR`, `ADMIN` | Interface d'ajout de films et planification de séances avec comptage d'activité |
| `POST` | `/programmation/films` | `PROGRAMMATEUR`, `ADMIN` | Formulaire d'enregistrement d'un nouveau film |
| `GET` | `/programmation/films/{id}/modifier` | `PROGRAMMATEUR`, `ADMIN` | Formulaire d'édition d'un film existant |
| `POST` | `/programmation/films/{id}/modifier` | `PROGRAMMATEUR`, `ADMIN` | Traitement de la mise à jour d'un film |
| `POST` | `/programmation/films/{id}/supprimer` | `PROGRAMMATEUR`, `ADMIN` | Suppression d'un film (rejet si des séances sont associées) |
| `POST` | `/programmation/seances` | `PROGRAMMATEUR`, `ADMIN` | Formulaire de planification d'une nouvelle séance |
| `GET` | `/programmation/seances/{id}/modifier` | `PROGRAMMATEUR`, `ADMIN` | Formulaire d'édition d'une séance |
| `POST` | `/programmation/seances/{id}/modifier` | `PROGRAMMATEUR`, `ADMIN` | Traitement de la mise à jour d'une séance |
| `POST` | `/programmation/seances/{id}/supprimer` | `PROGRAMMATEUR`, `ADMIN` | Suppression d'une séance (rejet si des réservations existent) |
| `GET` | `/admin/salles` | `ADMIN` | Interface de déclaration et gestion des salles physiques |
| `POST` | `/admin/salles` | `ADMIN` | Formulaire d'ajout d'une nouvelle salle physique |
| `GET` | `/admin/salles/{id}/modifier` | `ADMIN` | Formulaire d'édition d'une salle physique |
| `POST` | `/admin/salles/{id}/modifier` | `ADMIN` | Traitement de la mise à jour d'une salle physique |
| `POST` | `/admin/salles/{id}/supprimer` | `ADMIN` | Suppression d'une salle physique (rejet si des séances sont associées) |
| `GET` | `/admin/tableau-de-bord` | `ADMIN` | Tableau de bord de supervision (jauges et métriques Micrometer) |

### 7.2. API REST (Consommation programmatique)
| Méthode | URL | Accès / Rôle | Rôle fonctionnel | Codes HTTP attendus |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/films` | Public (Anonyme) | Liste l'ensemble des films à l'affiche | `200 OK` |
| `POST` | `/api/programmation/films` | `PROGRAMMATEUR`, `ADMIN` | Enregistre un nouveau film au catalogue | `201 Created`, `400 Bad Request` |
| `PUT` | `/api/programmation/films/{id}` | `PROGRAMMATEUR`, `ADMIN` | Met à jour un film existant | `200 OK`, `400 Bad Request`, `404 Not Found` |
| `DELETE` | `/api/programmation/films/{id}` | `PROGRAMMATEUR`, `ADMIN` | Supprime un film sans séance rattachée | `204 No Content`, `404 Not Found`, `409 Conflict` |
| `POST` | `/api/admin/salles` | `ADMIN` | Déclare une nouvelle salle physique | `201 Created`, `400 Bad Request`, `409 Conflict` |
| `PUT` | `/api/admin/salles/{id}` | `ADMIN` | Met à jour le nom et la capacité d'une salle | `200 OK`, `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `DELETE` | `/api/admin/salles/{id}` | `ADMIN` | Supprime une salle physique sans séance | `204 No Content`, `404 Not Found`, `409 Conflict` |
| `GET` | `/api/seances` | Public (Anonyme) | Liste les séances programmées | `200 OK` |
| `POST` | `/api/programmation/seances` | `PROGRAMMATEUR`, `ADMIN` | Programme une séance pour un film et une salle | `201 Created`, `400 Bad Request`, `404 Not Found` |
| `PUT` | `/api/programmation/seances/{id}` | `PROGRAMMATEUR`, `ADMIN` | Met à jour les paramètres d'une séance | `200 OK`, `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `DELETE` | `/api/programmation/seances/{id}` | `PROGRAMMATEUR`, `ADMIN` | Supprime une séance sans réservation active | `204 No Content`, `404 Not Found`, `409 Conflict` |
| `POST` | `/api/reservations` | `CLIENT`, `ADMIN` | Réserve des places sous contrôle de capacité | `201 Created`, `400 Bad Request`, `404 Not Found`, `409 Conflict` |
| `DELETE` | `/api/reservations/{code}` | `CLIENT`, `ADMIN` | Annule une réservation et libère les places | `204 No Content`, `404 Not Found` |
| `GET` | `/actuator/health` | Public (Anonyme) | Sonde de santé du service | `200 OK` (`{"status":"UP"}`) |
| `GET` | `/v3/api-docs` | Public (Anonyme) | Spécification OpenAPI 3.1 au format JSON | `200 OK` |
| `GET` | `/swagger-ui.html` | Public (Anonyme) | Interface Swagger UI interactive | `200 OK` / `302 Found` |

---

## 8. Stratégie de Concurrence & Invariant de Capacité

### Problématique
Dans un contexte de forte affluence (réservation des dernières places d'une séance), plusieurs transactions concurrentes peuvent tenter de réserver simultanément. Sans mécanisme d'isolation fort, des lectures concurrentes peuvent conclure à tort que des places sont libres, entraînant une surréservation (violation de l'invariant physique de la salle).

### Justification du Verrou Pessimiste (`PESSIMISTIC_WRITE`)
- **Limites du verrou optimiste (`@Version`) :** Avec un verrouillage optimiste, toutes les requêtes concurrentes lisent la même version de la séance. Une seule transaction valide son écriture lors du `COMMIT`, tandis que toutes les autres sont rejetées avec une `OptimisticLockException`. Cela impose à l'application une boucle de réessai (*retry*) complexe et coûteuse en ressources pour retenter la réservation jusqu'à épuisement.
- **Avantage du verrou pessimiste (`SELECT ... FOR UPDATE`) :** Dans [`ReservationService`](src/main/java/fr/univ/billetterie/service/ReservationService.java), l'appel `seanceRepository.findByIdWithLock(seanceId)` émet une clause SQL `SELECT ... FOR UPDATE`. Le moteur MySQL InnoDB pose un verrou exclusif sur la ligne d'index de la séance. Les transactions concurrentes sont ordonnancées séquentiellement par le moteur de base de données. Chaque transaction évalue exactement le cumul réel des places réservées (`sumPlacesReservees`). Si la demande dépasse le reliquat, l'exception métier `CapaciteInsuffisanteException` est levée immédiatement et la transaction est annulée sans aucun effet de bord, évitant toute surréservation sans nécessiter de retry client.

---

## 9. Bonus Métier : Métriques Micrometer

L'application intègre des métriques métier via le `MeterRegistry` de Micrometer (fourni par Spring Boot Actuator) :
- `billetterie.places.reservees` : compteur incrémenté du nombre de places pour chaque réservation confirmée.
- `billetterie.places.refusees` : compteur incrémenté du nombre de places demandées lors d'un refus pour capacité insuffisante.

Ces compteurs permettent de mesurer en direct la saturation des séances et la demande non servie sans impacter les performances transactionnelles.
