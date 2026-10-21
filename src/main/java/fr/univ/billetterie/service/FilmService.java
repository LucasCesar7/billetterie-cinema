package fr.univ.billetterie.service;

import fr.univ.billetterie.dto.FilmCreationRequest;
import fr.univ.billetterie.dto.FilmModificationRequest;
import fr.univ.billetterie.dto.FilmResponse;
import fr.univ.billetterie.entity.Film;
import fr.univ.billetterie.exception.ConflitMetierException;
import fr.univ.billetterie.exception.RessourceIntrouvableException;
import fr.univ.billetterie.repository.FilmRepository;
import fr.univ.billetterie.repository.SeanceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FilmService {

    private final FilmRepository filmRepository;
    private final SeanceRepository seanceRepository;

    public FilmService(FilmRepository filmRepository, SeanceRepository seanceRepository) {
        this.filmRepository = filmRepository;
        this.seanceRepository = seanceRepository;
    }

    @Transactional
    public FilmResponse creerFilm(FilmCreationRequest requete) {
        Film film = new Film(
                requete.titre(),
                requete.genre(),
                requete.dureeMinutes(),
                requete.dateSortie()
        );
        Film filmEnregistre = filmRepository.save(film);
        return versResponse(filmEnregistre);
    }

    @Transactional
    public FilmResponse modifierFilm(Long id, FilmModificationRequest request) {
        Film film = filmRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "FILM_INTROUVABLE",
                        "Le film avec l'identifiant " + id + " n'existe pas"
                ));

        film.setTitre(request.titre());
        film.setGenre(request.genre());
        film.setDureeMinutes(request.dureeMinutes());
        film.setDateSortie(request.dateSortie());

        Film filmMisAJour = filmRepository.save(film);
        return versResponse(filmMisAJour);
    }

    @Transactional
    public void supprimerFilm(Long id) {
        Film film = filmRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "FILM_INTROUVABLE",
                        "Le film avec l'identifiant " + id + " n'existe pas"
                ));

        if (seanceRepository.existsByFilmId(id)) {
            throw new ConflitMetierException(
                    "IMPOSSIBLE_SUPPRIMER_FILM",
                    "Impossible de supprimer ce film : des séances lui sont associées"
            );
        }

        filmRepository.delete(film);
    }

    @Transactional(readOnly = true)
    public Film trouverFilm(Long id) {
        return filmRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "FILM_INTROUVABLE",
                        "Le film avec l'identifiant " + id + " n'existe pas"
                ));
    }

    @Transactional(readOnly = true)
    public List<FilmResponse> rechercherFilms(String titre, String genre) {
        boolean aTitre = titre != null && !titre.isBlank();
        boolean aGenre = genre != null && !genre.isBlank();

        List<Film> films;
        if (aTitre && aGenre) {
            films = filmRepository.findByTitreContainingIgnoreCaseOrGenreContainingIgnoreCase(titre.trim(), genre.trim());
        } else if (aTitre) {
            films = filmRepository.findByTitreContainingIgnoreCase(titre.trim());
        } else if (aGenre) {
            films = filmRepository.findByGenreContainingIgnoreCase(genre.trim());
        } else {
            films = filmRepository.findAll();
        }

        return films.stream().map(this::versResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<Film> listerTousLesFilms() {
        return filmRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long compterSeancesPourFilm(Long filmId) {
        return seanceRepository.countByFilmId(filmId);
    }

    public FilmResponse versResponse(Film film) {
        return new FilmResponse(
                film.getId(),
                film.getTitre(),
                film.getGenre(),
                film.getDureeMinutes(),
                film.getDateSortie()
        );
    }
}
