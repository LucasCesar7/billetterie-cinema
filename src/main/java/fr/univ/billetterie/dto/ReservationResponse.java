package fr.univ.billetterie.dto;

import fr.univ.billetterie.entity.StatutReservation;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
        String code,
        StatutReservation statut,
        Integer nombrePlaces,
        BigDecimal montantTotal,
        Long seanceId,
        LocalDateTime creeeLe
) {
}

