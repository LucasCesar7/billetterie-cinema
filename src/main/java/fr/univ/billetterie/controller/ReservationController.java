package fr.univ.billetterie.controller;

import fr.univ.billetterie.dto.ReservationCreationRequest;
import fr.univ.billetterie.dto.ReservationResponse;
import fr.univ.billetterie.service.ReservationService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/api/reservations")
    public ResponseEntity<ReservationResponse> creerReservation(@Valid @RequestBody ReservationCreationRequest requete) {
        ReservationResponse reponse = reservationService.creerReservation(requete);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/reservations/{code}")
                .buildAndExpand(reponse.code())
                .toUri();
        return ResponseEntity.created(location).body(reponse);
    }

    @DeleteMapping("/api/reservations/{code}")
    public ResponseEntity<Void> annulerReservation(@PathVariable String code) {
        reservationService.annulerReservation(code);
        return ResponseEntity.noContent().build();
    }
}

