package fr.univ.billetterie.dto.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BilletConfirmationVue(
        String code,
        String emailClient,
        int nombrePlaces,
        BigDecimal montantTotal,
        String filmTitre,
        String nomSalle,
        LocalDateTime debutSeance,
        String dateFormatee,
        String heureFormatee,
        String statut,
        LocalDateTime creeeLe,
        boolean annulee
) {
}

