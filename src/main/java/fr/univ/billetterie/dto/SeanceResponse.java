package fr.univ.billetterie.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SeanceResponse(
        Long id,
        String filmTitre,
        String salleNom,
        LocalDateTime debut,
        BigDecimal prixUnitaire
) {
}

