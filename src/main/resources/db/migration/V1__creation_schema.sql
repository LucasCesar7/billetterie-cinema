-- V1 : Création du schéma relationnel de la billetterie de cinéma

CREATE TABLE film (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    genre VARCHAR(100),
    duree_minutes INT NOT NULL,
    date_sortie DATE,
    CONSTRAINT chk_film_duree CHECK (duree_minutes BETWEEN 40 AND 300)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE salle (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL UNIQUE,
    capacite INT NOT NULL,
    CONSTRAINT chk_salle_capacite CHECK (capacite BETWEEN 20 AND 500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE seance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    film_id BIGINT NOT NULL,
    salle_id BIGINT NOT NULL,
    debut DATETIME NOT NULL,
    prix_unitaire DECIMAL(6, 2) NOT NULL,
    CONSTRAINT chk_seance_prix CHECK (prix_unitaire > 0),
    CONSTRAINT fk_seance_film FOREIGN KEY (film_id) REFERENCES film (id) ON DELETE RESTRICT,
    CONSTRAINT fk_seance_salle FOREIGN KEY (salle_id) REFERENCES salle (id) ON DELETE RESTRICT,
    INDEX idx_seance_debut (debut),
    INDEX idx_seance_film_id (film_id),
    INDEX idx_seance_salle_id (salle_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    seance_id BIGINT NOT NULL,
    email_client VARCHAR(255) NOT NULL,
    nombre_places INT NOT NULL,
    statut VARCHAR(20) NOT NULL,
    creee_le DATETIME NOT NULL,
    CONSTRAINT fk_reservation_seance FOREIGN KEY (seance_id) REFERENCES seance (id) ON DELETE RESTRICT,
    CONSTRAINT chk_reservation_places CHECK (nombre_places BETWEEN 1 AND 8),
    CONSTRAINT chk_reservation_statut CHECK (statut IN ('CONFIRMEE', 'ANNULEE')),
    INDEX idx_reservation_seance_id (seance_id),
    INDEX idx_reservation_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

