# Backend Billetterie Cinéma - Haute Fiabilité (R5.08)

Backend de billetterie de cinéma développé en **Java 25**, **Spring Boot 4.1.1**, **MySQL 8.4** et **Flyway**, conçu pour garantir un invariant strict de capacité même sous forte contention multi-threadée.

---

## 1. Présentation & Philosophie d'Ingénierie

### Contexte technique
Ce projet implémente l'API d'un système de billetterie de cinéma haute fiabilité. Il repose sur un modèle relationnel strict, une étanchéité totale des couches par DTOs (`record` Java 25 natifs), une sécurité HTTP Basic sans état (stateless), ainsi qu'une sérialisation des réservations par verrouillage pessimiste au niveau de MySQL InnoDB.

### Méthodologie de développement assisté par IA
Le développement a été réalisé en binôme avec l'agent autonome Antigravity (piloté par Gemini 3.8 Flash) sous VS Code, selon un cadre d'ingénierie rigoureux :
- **Cadrage humain préalable :** Rédaction d'un fichier de règles strictes ([`.agentrules`](.agentrules)) imposant du code 100 % en français, zéro processeur d'annotations tiers (zéro Lombok), l'exclusion totale de bases simulées comme H2, et l'usage exclusif de conteneurs MySQL 8.4 via Testcontainers.
- **Découpage modulaire par phases :** Progression incrémentale par prompts structurés (Phase 1 : socle et wrapper, Phase 2 : persistance et Flyway, Phase 3 : API REST et sécurité, Phase 4 : concurrence et recette A8, Phase 5 : exploitation, métriques et livrable).
- **Zéro génération aveugle :** Chaque phase a fait l'objet d'une compilation du bytecode, d'une isolation sur branche Git dédiée, d'une exécution de tests automatisés sous conteneur réel et d'une consignation dans un journal persistant ([`MEMOIRE.md`](MEMOIRE.md)) pour prévenir toute régression.

---

## 2. Vision Ergonomie & Futur Front-End

L'API et ses DTOs ont été modélisés pour alimenter un futur front-end web épuré, réactif et accessible :
- **Rejet du design générique IA :** Refus des interfaces stéréotypées (cartes surchargées, dégradés violets superflus, animations distrayantes).
- **Parcours utilisateur fluide en 3 étapes :**
  1. *Sélection du film et de la séance* : affichage clair des horaires, du genre et du tarif unitaire.
  2. *Choix des places avec jauge en direct* : visualisation en temps réel du nombre de places restantes avant saturation.
  3. *Confirmation instantanée* : récapitulatif avec code unique de réservation (`RES-XXXXXXXXXX`), statut confirmé et montant total débité en `BigDecimal`.
- **Contrat d'API robuste :** Les réponses d'erreur (`ErreurResponse`) fournissent un code d'erreur textuel stable (`CAPACITE_INSUFFISANTE`, `VALIDATION_ECHOUEE`, `RESSOURCE_INTROUVABLE`) permettant au front-end d'afficher des notifications utilisateur précises sans interpréter de code technique interne.

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
Lance l'ensemble des tests (schéma, contrôleurs, sécurité, concurrence multi-threadée, exploitation) sous conteneurs MySQL réels :

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

## 6. Comptes de Démonstration (Authentification HTTP Basic)

L'API utilise une authentification HTTP Basic sans état avec mots de passe hachés via BCrypt :

| Identifiant | Mot de passe | Rôle Spring Security | Périmètre d'action |
| :--- | :--- | :--- | :--- |
| `client` | `client123` | `ROLE_CLIENT` | Effectuer une réservation (`POST /api/reservations`), annuler sa réservation (`DELETE /api/reservations/{code}`). |
| `programmateur` | `prog123` | `ROLE_PROGRAMMATEUR` | Créer un film (`POST /api/films`), programmer une séance (`POST /api/seances`). |
| `admin` | `admin123` | `ROLE_ADMIN` | Créer une salle (`POST /api/salles`), accès complet à l'ensemble des endpoints. |

---

## 7. Tableau Récapitulatif de l'API REST

| Méthode | URL | Accès / Rôle | Rôle fonctionnel | Codes HTTP attendus |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/films` | Public (Anonyme) | Liste l'ensemble des films à l'affiche | `200 OK` |
| `POST` | `/api/films` | `PROGRAMMATEUR`, `ADMIN` | Enregistre un nouveau film au catalogue | `201 Created`, `400 Bad Request` |
| `POST` | `/api/salles` | `ADMIN` | Déclare une nouvelle salle physique | `201 Created`, `400 Bad Request`, `409 Conflict` |
| `GET` | `/api/seances` | Public (Anonyme) | Liste les séances programmées | `200 OK` |
| `POST` | `/api/seances` | `PROGRAMMATEUR`, `ADMIN` | Programme une séance pour un film et une salle | `201 Created`, `400 Bad Request`, `404 Not Found` |
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
