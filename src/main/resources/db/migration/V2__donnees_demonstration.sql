-- V2 : Données de démonstration pour la billetterie de cinéma

INSERT INTO film (titre, genre, duree_minutes, date_sortie) VALUES
('Inception', 'Science-Fiction', 148, '2010-07-21'),
('Interstellar', 'Science-Fiction', 169, '2014-11-05');

INSERT INTO salle (nom, capacite) VALUES
('Salle 1', 100);

-- Séance de démonstration pour le premier film dans la salle 1
INSERT INTO seance (film_id, salle_id, debut, prix_unitaire) VALUES
(1, 1, '2026-11-01 20:00:00', 10.50);

