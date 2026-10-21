package fr.univ.billetterie.service;

import fr.univ.billetterie.dto.SeanceCreationRequest;
import fr.univ.billetterie.dto.SeanceModificationRequest;
import fr.univ.billetterie.dto.SeanceResponse;
import fr.univ.billetterie.entity.Film;
import fr.univ.billetterie.entity.Salle;
import fr.univ.billetterie.entity.Seance;
import fr.univ.billetterie.entity.StatutReservation;
import fr.univ.billetterie.exception.CapaciteInsuffisanteException;
import fr.univ.billetterie.exception.ConflitMetierException;
import fr.univ.billetterie.exception.RessourceIntrouvableException;
import fr.univ.billetterie.repository.FilmRepository;
import fr.univ.billetterie.repository.ReservationRepository;
import fr.univ.billetterie.repository.SalleRepository;
import fr.univ.billetterie.repository.SeanceRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeanceService {

    private final SeanceRepository seanceRepository;
    private final FilmRepository filmRepository;
    private final SalleRepository salleRepository;
    private final ReservationRepository reservationRepository;

    public SeanceService(
            SeanceRepository seanceRepository,
            FilmRepository filmRepository,
            SalleRepository salleRepository,
            ReservationRepository reservationRepository
    ) {
        this.seanceRepository = seanceRepository;
        this.filmRepository = filmRepository;
        this.salleRepository = salleRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public SeanceResponse creerSeance(SeanceCreationRequest requete) {
        Film film = filmRepository.findById(requete.filmId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "FILM_INTROUVABLE",
                        "Le film avec l'identifiant " + requete.filmId() + " n'existe pas"
                ));

        Salle salle = salleRepository.findById(requete.salleId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "SALLE_INTROUVABLE",
                        "La salle avec l'identifiant " + requete.salleId() + " n'existe pas"
                ));

        Seance seance = new Seance(film, salle, requete.debut(), requete.prixUnitaire());
        Seance seanceEnregistree = seanceRepository.save(seance);
        return versResponse(seanceEnregistree);
    }

    @Transactional
    public SeanceResponse modifierSeance(Long id, SeanceModificationRequest requete) {
        Seance seance = seanceRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "SEANCE_INTROUVABLE",
                        "La séance avec l'identifiant " + id + " n'existe pas"
                ));

        Film film = filmRepository.findById(requete.filmId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "FILM_INTROUVABLE",
                        "Le film avec l'identifiant " + requete.filmId() + " n'existe pas"
                ));

        Salle salle = salleRepository.findById(requete.salleId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "SALLE_INTROUVABLE",
                        "La salle avec l'identifiant " + requete.salleId() + " n'existe pas"
                ));

        int placesReservees = reservationRepository.sumPlacesReservees(id, StatutReservation.CONFIRMEE);
        if (salle.getCapacite() < placesReservees) {
            throw new CapaciteInsuffisanteException(
                    "CAPACITE_INSUFFISANTE",
                    "La nouvelle salle '" + salle.getNom() + "' (" + salle.getCapacite() + " places) ne peut pas accueillir les " + placesReservees + " places déjà confirmées"
            );
        }

        seance.setFilm(film);
        seance.setSalle(salle);
        seance.setDebut(requete.debut());
        seance.setPrixUnitaire(requete.prixUnitaire());

        Seance seanceSauvegardee = seanceRepository.save(seance);
        return versResponse(seanceSauvegardee);
    }

    @Transactional
    public void supprimerSeance(Long id) {
        Seance seance = seanceRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "SEANCE_INTROUVABLE",
                        "La séance avec l'identifiant " + id + " n'existe pas"
                ));

        if (reservationRepository.existsBySeanceId(id)) {
            throw new ConflitMetierException(
                    "IMPOSSIBLE_SUPPRIMER_SEANCE",
                    "Impossible de supprimer cette séance : des réservations y sont rattachées"
            );
        }

        seanceRepository.delete(seance);
    }

    @Transactional(readOnly = true)
    public List<SeanceResponse> rechercherSeances(LocalDateTime du, LocalDateTime au) {
        List<Seance> seances;
        if (du != null && au != null) {
            seances = seanceRepository.findByDebutBetween(du, au);
        } else if (du != null) {
            seances = seanceRepository.findByDebutBetween(du, LocalDateTime.of(9999, 12, 31, 23, 59));
        } else if (au != null) {
            seances = seanceRepository.findByDebutBetween(LocalDateTime.of(1970, 1, 1, 0, 0), au);
        } else {
            seances = seanceRepository.findAll();
        }

        return seances.stream().map(this::versResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<Seance> listerToutesLesSeances() {
        return seanceRepository.findAllWithFilmAndSalle();
    }

    @Transactional(readOnly = true)
    public List<Seance> listerSeancesParFilm(Long filmId) {
        return seanceRepository.findByFilmIdWithFilmAndSalle(filmId);
    }

    @Transactional(readOnly = true)
    public Seance trouverSeance(Long id) {
        return seanceRepository.findByIdWithFilmAndSalle(id)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "SEANCE_INTROUVABLE",
                        "La séance avec l'identifiant " + id + " n'existe pas"
                ));
    }

    @Transactional(readOnly = true)
    public long compterReservationsPourSeance(Long seanceId) {
        return reservationRepository.countBySeanceId(seanceId);
    }

    public SeanceResponse versResponse(Seance seance) {
        return new SeanceResponse(
                seance.getId(),
                seance.getFilm().getTitre(),
                seance.getSalle().getNom(),
                seance.getDebut(),
                seance.getPrixUnitaire()
        );
    }
}
