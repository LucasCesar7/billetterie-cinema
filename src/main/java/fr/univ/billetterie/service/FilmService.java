package fr.univ.billetterie.service;

import fr.univ.billetterie.dto.FilmCreationRequest;
import fr.univ.billetterie.dto.FilmResponse;
import fr.univ.billetterie.entity.Film;
import fr.univ.billetterie.repository.FilmRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FilmService {

    private final FilmRepository filmRepository;

    public FilmService(FilmRepository filmRepository) {
        this.filmRepository = filmRepository;
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

