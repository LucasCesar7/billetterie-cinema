package fr.univ.billetterie.dto.web;

import java.time.LocalDate;
import java.util.List;

public record FilmCatalogueVue(
        Long id,
        String titre,
        String genre,
        Integer dureeMinutes,
        String dureeFormatee,
        LocalDate dateSortie,
        List<SeanceResumeVue> seances
) {
}

