package fr.univ.billetterie.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record FilmModificationRequest(
        @NotBlank(message = "Le titre du film est obligatoire")
        String titre,

        String genre,

        @NotNull(message = "La durée est obligatoire")
        @Min(value = 40, message = "La durée minimale d'un film est de 40 minutes")
        @Max(value = 300, message = "La durée maximale d'un film est de 300 minutes")
        Integer dureeMinutes,

        @NotNull(message = "La date de sortie est obligatoire")
        LocalDate dateSortie
) {
}

