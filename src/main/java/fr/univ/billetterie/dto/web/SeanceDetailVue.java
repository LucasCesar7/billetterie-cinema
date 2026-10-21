package fr.univ.billetterie.dto.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SeanceDetailVue(
        Long id,
        Long filmId,
        String filmTitre,
        String filmGenre,
        Integer filmDureeMinutes,
        String filmDureeFormatee,
        String nomSalle,
        int capaciteSalle,
        LocalDateTime debut,
        String dateFormatee,
        String heureFormatee,
        BigDecimal prixUnitaire,
        int placesReservees,
        int placesRestantes,
        int pourcentageOccupation,
        boolean complete
) {
}

