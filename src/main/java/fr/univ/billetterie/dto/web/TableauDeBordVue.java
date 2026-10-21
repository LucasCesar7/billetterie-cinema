package fr.univ.billetterie.dto.web;

import java.util.List;

public record TableauDeBordVue(
        long totalPlacesReservees,
        long totalPlacesRefusees,
        int nombreFilms,
        int nombreSeances,
        List<SeanceResumeVue> seances
) {
}

