package fr.univ.billetterie.dto.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SeanceResumeVue(
        Long id,
        Long filmId,
        String filmTitre,
        LocalDateTime debut,
        String dateFormatee,
        String heureFormatee,
        BigDecimal prixUnitaire,
        String nomSalle,
        int capaciteSalle,
        int placesReservees,
        int placesRestantes,
        int pourcentageOccupation,
        boolean complete
) {
}

