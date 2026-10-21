package fr.univ.billetterie.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SeanceModificationRequest(
        @NotNull(message = "L'identifiant du film est obligatoire")
        Long filmId,

        @NotNull(message = "L'identifiant de la salle est obligatoire")
        Long salleId,

        @NotNull(message = "L'horaire de début est obligatoire")
        LocalDateTime debut,

        @NotNull(message = "Le prix unitaire est obligatoire")
        @Positive(message = "Le prix unitaire doit être positif")
        BigDecimal prixUnitaire
) {
}

