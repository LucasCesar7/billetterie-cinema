package fr.univ.billetterie.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SalleModificationRequest(
        @NotBlank(message = "Le nom de la salle est obligatoire")
        String nom,

        @NotNull(message = "La capacité est obligatoire")
        @Min(value = 20, message = "La capacité minimale est de 20 places")
        @Max(value = 500, message = "La capacité maximale est de 500 places")
        Integer capacite
) {
}

