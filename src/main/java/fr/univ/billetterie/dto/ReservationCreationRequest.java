package fr.univ.billetterie.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReservationCreationRequest(
        @NotNull(message = "L'identifiant de la séance est obligatoire")
        Long seanceId,

        @NotBlank(message = "L'adresse email est obligatoire")
        @Email(message = "L'adresse email doit être valide")
        String emailClient,

        @NotNull(message = "Le nombre de places est obligatoire")
        @Min(value = 1, message = "Le nombre de places doit être au minimum de 1")
        @Max(value = 8, message = "Le nombre de places doit être au maximum de 8")
        Integer nombrePlaces
) {
}

