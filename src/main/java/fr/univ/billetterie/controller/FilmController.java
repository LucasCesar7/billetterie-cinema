package fr.univ.billetterie.controller;

import fr.univ.billetterie.dto.FilmCreationRequest;
import fr.univ.billetterie.dto.FilmModificationRequest;
import fr.univ.billetterie.dto.FilmResponse;
import fr.univ.billetterie.service.FilmService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class FilmController {

    private final FilmService filmService;

    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @GetMapping("/api/films")
    public ResponseEntity<List<FilmResponse>> obtenirFilms(
            @RequestParam(required = false) String titre,
            @RequestParam(required = false) String genre
    ) {
        List<FilmResponse> films = filmService.rechercherFilms(titre, genre);
        return ResponseEntity.ok(films);
    }

    @PostMapping("/api/programmation/films")
    public ResponseEntity<FilmResponse> creerFilm(@Valid @RequestBody FilmCreationRequest requete) {
        FilmResponse reponse = filmService.creerFilm(requete);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/films/{id}")
                .buildAndExpand(reponse.id())
                .toUri();
        return ResponseEntity.created(location).body(reponse);
    }

    @PutMapping("/api/programmation/films/{id}")
    public ResponseEntity<FilmResponse> modifierFilm(
            @PathVariable Long id,
            @Valid @RequestBody FilmModificationRequest requete
    ) {
        FilmResponse reponse = filmService.modifierFilm(id, requete);
        return ResponseEntity.ok(reponse);
    }

    @DeleteMapping("/api/programmation/films/{id}")
    public ResponseEntity<Void> supprimerFilm(@PathVariable Long id) {
        filmService.supprimerFilm(id);
        return ResponseEntity.noContent().build();
    }
}
