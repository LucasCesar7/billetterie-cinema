package fr.univ.billetterie.service;

import fr.univ.billetterie.dto.SeanceCreationRequest;
import fr.univ.billetterie.dto.SeanceResponse;
import fr.univ.billetterie.entity.Film;
import fr.univ.billetterie.entity.Salle;
import fr.univ.billetterie.entity.Seance;
import fr.univ.billetterie.exception.RessourceIntrouvableException;
import fr.univ.billetterie.repository.FilmRepository;
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

    public SeanceService(
            SeanceRepository seanceRepository,
            FilmRepository filmRepository,
            SalleRepository salleRepository
    ) {
        this.seanceRepository = seanceRepository;
        this.filmRepository = filmRepository;
        this.salleRepository = salleRepository;
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

