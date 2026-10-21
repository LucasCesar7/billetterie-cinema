package fr.univ.billetterie.dto.web;

import java.time.LocalDate;

public record FilmLigneVue(
        Long id,
        String titre,
        String genre,
        Integer dureeMinutes,
        String dureeFormatee,
        LocalDate dateSortie,
        long nombreSeances
) {
}

