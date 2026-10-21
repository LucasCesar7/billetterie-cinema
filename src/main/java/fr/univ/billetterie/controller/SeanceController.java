package fr.univ.billetterie.controller;

import fr.univ.billetterie.dto.SeanceCreationRequest;
import fr.univ.billetterie.dto.SeanceResponse;
import fr.univ.billetterie.service.SeanceService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class SeanceController {

    private final SeanceService seanceService;

    public SeanceController(SeanceService seanceService) {
        this.seanceService = seanceService;
    }

    @GetMapping("/api/seances")
    public ResponseEntity<List<SeanceResponse>> obtenirSeances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime au
    ) {
        List<SeanceResponse> seances = seanceService.rechercherSeances(du, au);
        return ResponseEntity.ok(seances);
    }

    @PostMapping("/api/programmation/seances")
    public ResponseEntity<SeanceResponse> creerSeance(@Valid @RequestBody SeanceCreationRequest requete) {
        SeanceResponse reponse = seanceService.creerSeance(requete);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/seances/{id}")
                .buildAndExpand(reponse.id())
                .toUri();
        return ResponseEntity.created(location).body(reponse);
    }
}

