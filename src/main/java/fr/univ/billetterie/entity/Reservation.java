package fr.univ.billetterie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seance_id", nullable = false)
    private Seance seance;

    @Column(name = "email_client", nullable = false)
    private String emailClient;

    @Column(name = "nombre_places", nullable = false)
    private Integer nombrePlaces;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutReservation statut;

    @Column(name = "creee_le", nullable = false)
    private LocalDateTime creeeLe;

    public Reservation() {
    }

    public Reservation(String code, Seance seance, String emailClient, Integer nombrePlaces, StatutReservation statut, LocalDateTime creeeLe) {
        this.code = code;
        this.seance = seance;
        this.emailClient = emailClient;
        this.nombrePlaces = nombrePlaces;
        this.statut = statut;
        this.creeeLe = creeeLe;
    }

    public Reservation(Long id, String code, Seance seance, String emailClient, Integer nombrePlaces, StatutReservation statut, LocalDateTime creeeLe) {
        this.id = id;
        this.code = code;
        this.seance = seance;
        this.emailClient = emailClient;
        this.nombrePlaces = nombrePlaces;
        this.statut = statut;
        this.creeeLe = creeeLe;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Seance getSeance() {
        return seance;
    }

    public void setSeance(Seance seance) {
        this.seance = seance;
    }

    public String getEmailClient() {
        return emailClient;
    }

    public void setEmailClient(String emailClient) {
        this.emailClient = emailClient;
    }

    public Integer getNombrePlaces() {
        return nombrePlaces;
    }

    public void setNombrePlaces(Integer nombrePlaces) {
        this.nombrePlaces = nombrePlaces;
    }

    public StatutReservation getStatut() {
        return statut;
    }

    public void setStatut(StatutReservation statut) {
        this.statut = statut;
    }

    public LocalDateTime getCreeeLe() {
        return creeeLe;
    }

    public void setCreeeLe(LocalDateTime creeeLe) {
        this.creeeLe = creeeLe;
    }
}

