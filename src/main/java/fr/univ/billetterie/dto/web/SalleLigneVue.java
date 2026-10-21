package fr.univ.billetterie.dto.web;

public record SalleLigneVue(
        Long id,
        String nom,
        int capacite,
        long nombreSeances
) {
}
