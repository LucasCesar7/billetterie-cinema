package fr.univ.billetterie.controller;

import fr.univ.billetterie.dto.SalleCreationRequest;
import fr.univ.billetterie.dto.SalleResponse;
import fr.univ.billetterie.service.SalleService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class SalleController {

    private final SalleService salleService;

    public SalleController(SalleService salleService) {
        this.salleService = salleService;
    }

    @PostMapping("/api/admin/salles")
    public ResponseEntity<SalleResponse> creerSalle(@Valid @RequestBody SalleCreationRequest requete) {
        SalleResponse reponse = salleService.creerSalle(requete);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/salles/{id}")
                .buildAndExpand(reponse.id())
                .toUri();
        return ResponseEntity.created(location).body(reponse);
    }
}

