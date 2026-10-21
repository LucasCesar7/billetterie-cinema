package fr.univ.billetterie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "film")
public class Film {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titre", nullable = false)
    private String titre;

    @Column(name = "genre", length = 100)
    private String genre;

    @Column(name = "duree_minutes", nullable = false)
    private Integer dureeMinutes;

    @Column(name = "date_sortie")
    private LocalDate dateSortie;

    public Film() {
    }

    public Film(String titre, String genre, Integer dureeMinutes, LocalDate dateSortie) {
        this.titre = titre;
        this.genre = genre;
        this.dureeMinutes = dureeMinutes;
        this.dateSortie = dateSortie;
    }

    public Film(Long id, String titre, String genre, Integer dureeMinutes, LocalDate dateSortie) {
        this.id = id;
        this.titre = titre;
        this.genre = genre;
        this.dureeMinutes = dureeMinutes;
        this.dateSortie = dateSortie;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getDureeMinutes() {
        return dureeMinutes;
    }

    public void setDureeMinutes(Integer dureeMinutes) {
        this.dureeMinutes = dureeMinutes;
    }

    public LocalDate getDateSortie() {
        return dateSortie;
    }

    public void setDateSortie(LocalDate dateSortie) {
        this.dateSortie = dateSortie;
    }
}

