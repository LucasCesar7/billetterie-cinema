package fr.univ.billetterie.service;

import fr.univ.billetterie.dto.ReservationCreationRequest;
import fr.univ.billetterie.dto.ReservationResponse;
import fr.univ.billetterie.entity.Reservation;
import fr.univ.billetterie.entity.Seance;
import fr.univ.billetterie.entity.StatutReservation;
import fr.univ.billetterie.exception.CapaciteInsuffisanteException;
import fr.univ.billetterie.exception.RessourceIntrouvableException;
import fr.univ.billetterie.repository.ReservationRepository;
import fr.univ.billetterie.repository.SeanceRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SeanceRepository seanceRepository;
    private final Counter compteurPlacesReservees;
    private final Counter compteurPlacesRefusees;

    public ReservationService(
            ReservationRepository reservationRepository,
            SeanceRepository seanceRepository,
            MeterRegistry meterRegistry
    ) {
        this.reservationRepository = reservationRepository;
        this.seanceRepository = seanceRepository;
        this.compteurPlacesReservees = Counter.builder("billetterie.places.reservees")
                .description("Nombre total de places confirmées lors des réservations")
                .register(meterRegistry);
        this.compteurPlacesRefusees = Counter.builder("billetterie.places.refusees")
                .description("Nombre total de places refusées pour cause de capacité insuffisante")
                .register(meterRegistry);
    }

    @Transactional
    public ReservationResponse creerReservation(ReservationCreationRequest requete) {
        Seance seance = seanceRepository.findByIdWithLock(requete.seanceId())
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "SEANCE_INTROUVABLE",
                        "La séance avec l'identifiant " + requete.seanceId() + " n'existe pas"
                ));

        int placesReservees = reservationRepository.sumPlacesReservees(seance.getId(), StatutReservation.CONFIRMEE);
        int placesRestantes = seance.getSalle().getCapacite() - placesReservees;

        if (requete.nombrePlaces() > placesRestantes) {
            compteurPlacesRefusees.increment(requete.nombrePlaces());
            throw new CapaciteInsuffisanteException(
                    "CAPACITE_INSUFFISANTE",
                    "Il ne reste que " + placesRestantes + " places pour cette séance"
            );
        }

        String code = "RES-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        BigDecimal montantTotal = seance.getPrixUnitaire().multiply(BigDecimal.valueOf(requete.nombrePlaces()));

        Reservation reservation = new Reservation(
                code,
                seance,
                requete.emailClient(),
                requete.nombrePlaces(),
                StatutReservation.CONFIRMEE,
                LocalDateTime.now()
        );

        Reservation reservationEnregistree = reservationRepository.save(reservation);
        compteurPlacesReservees.increment(requete.nombrePlaces());

        return new ReservationResponse(
                reservationEnregistree.getCode(),
                reservationEnregistree.getStatut(),
                reservationEnregistree.getNombrePlaces(),
                montantTotal,
                seance.getId(),
                reservationEnregistree.getCreeeLe()
        );
    }

    @Transactional
    public void annulerReservation(String code) {
        Reservation reservation = reservationRepository.findByCode(code)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "RESERVATION_INTROUVABLE",
                        "La réservation avec le code " + code + " n'existe pas"
                ));

        reservation.setStatut(StatutReservation.ANNULEE);
        reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public Reservation trouverReservation(String code) {
        return reservationRepository.findByCodeWithSeanceAndFilmAndSalle(code)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "RESERVATION_INTROUVABLE",
                        "La réservation avec le code " + code + " n'existe pas"
                ));
    }

    public int compterPlacesRestantes(Long seanceId, int capaciteSalle) {
        int placesReservees = reservationRepository.sumPlacesReservees(seanceId, StatutReservation.CONFIRMEE);
        return Math.max(0, capaciteSalle - placesReservees);
    }

    public int compterPlacesReservees(Long seanceId) {
        return reservationRepository.sumPlacesReservees(seanceId, StatutReservation.CONFIRMEE);
    }
}
