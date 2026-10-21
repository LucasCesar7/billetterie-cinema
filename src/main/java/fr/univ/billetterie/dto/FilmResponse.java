package fr.univ.billetterie.dto;

import java.time.LocalDate;

public record FilmResponse(
        Long id,
        String titre,
        String genre,
        Integer dureeMinutes,
        LocalDate dateSortie
) {
}

