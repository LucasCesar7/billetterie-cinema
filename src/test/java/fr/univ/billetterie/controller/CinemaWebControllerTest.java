package fr.univ.billetterie.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import fr.univ.billetterie.entity.Film;
import fr.univ.billetterie.entity.Reservation;
import fr.univ.billetterie.entity.Salle;
import fr.univ.billetterie.entity.Seance;
import fr.univ.billetterie.entity.StatutReservation;
import fr.univ.billetterie.repository.FilmRepository;
import fr.univ.billetterie.repository.ReservationRepository;
import fr.univ.billetterie.repository.SalleRepository;
import fr.univ.billetterie.repository.SeanceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CinemaWebControllerTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("billetterie_db")
            .withUsername("cinema_user")
            .withPassword("cinema_pass");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private SalleRepository salleRepository;

    @Autowired
    private SeanceRepository seanceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Film filmTest;
    private Salle salleTest;
    private Seance seanceTest;

    @BeforeEach
    void initialiserDonnees() {
        reservationRepository.deleteAll();
        seanceRepository.deleteAll();

        filmTest = filmRepository.findAll().stream().findFirst().orElseGet(() ->
                filmRepository.save(new Film("Dune : Deuxième Partie", "Science-Fiction", 166, LocalDate.of(2024, 2, 28)))
        );

        salleTest = salleRepository.findAll().stream().findFirst().orElseGet(() ->
                salleRepository.save(new Salle("Salle IMAX", 50))
        );

        seanceTest = seanceRepository.save(new Seance(
                filmTest,
                salleTest,
                LocalDateTime.now().plusDays(2).withHour(20).withMinute(0).withSecond(0).withNano(0),
                BigDecimal.valueOf(12.50)
        ));
    }

    @Test
    @DisplayName("GET / et /films : affiche le catalogue avec statut 200 et modèle films")
    void afficherCatalogue_devraitRenvoyerVueIndex() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("films"));

        mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("films"));
    }

    @Test
    @DisplayName("GET /connexion : affiche la page d'authentification avec statut 200")
    void afficherPageConnexion_devraitRenvoyerVueConnexion() throws Exception {
        mockMvc.perform(get("/connexion"))
                .andExpect(status().isOk())
                .andExpect(view().name("connexion"));
    }

    @Test
    @DisplayName("GET /seances/{id}/reserver : affiche le formulaire de réservation de la séance")
    void afficherPageReservation_devraitRenvoyerVueReservation() throws Exception {
        mockMvc.perform(get("/seances/" + seanceTest.getId() + "/reserver"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservation"))
                .andExpect(model().attributeExists("seance"));
    }

    @Test
    @DisplayName("POST /reserver : valide la réservation et redirige vers /confirmation/{code}")
    void traiterReservation_nominal_devraitRedirigerVersConfirmation() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/reserver")
                        .param("seanceId", seanceTest.getId().toString())
                        .param("emailClient", "jean.dupont@test.fr")
                        .param("nombrePlaces", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/confirmation/RES-*"))
                .andReturn();

        String urlRedirection = resultat.getResponse().getRedirectedUrl();
        assertThat(urlRedirection).isNotNull();
        String code = urlRedirection.replace("/confirmation/", "");

        Reservation reservation = reservationRepository.findByCode(code).orElseThrow();
        assertThat(reservation.getNombrePlaces()).isEqualTo(3);
        assertThat(reservation.getEmailClient()).isEqualTo("jean.dupont@test.fr");
        assertThat(reservation.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
    }

    @Test
    @DisplayName("POST /reserver : en cas de surréservation, réaffiche la vue avec alerte d'erreur")
    void traiterReservation_surcapacite_devraitReafficherVueAvecErreur() throws Exception {
        mockMvc.perform(post("/reserver")
                        .param("seanceId", seanceTest.getId().toString())
                        .param("emailClient", "surreservation@test.fr")
                        .param("nombrePlaces", "999"))
                .andExpect(status().isOk())
                .andExpect(view().name("reservation"))
                .andExpect(model().attributeExists("erreur", "seance"));
    }

    @Test
    @DisplayName("GET /confirmation/{code} : affiche le billet numérique pour un code existant")
    void afficherConfirmation_devraitRenvoyerVueBillet() throws Exception {
        Reservation reservation = reservationRepository.save(new Reservation(
                "RES-TESTBILLET1",
                seanceTest,
                "sophie.martin@test.fr",
                2,
                StatutReservation.CONFIRMEE,
                LocalDateTime.now()
        ));

        mockMvc.perform(get("/confirmation/" + reservation.getCode()))
                .andExpect(status().isOk())
                .andExpect(view().name("confirmation"))
                .andExpect(model().attributeExists("billet"));
    }

    @Test
    @DisplayName("POST /annuler/{code} : annule la réservation et redirige vers l'accueil")
    void annulerReservation_devraitAnnulerEtRedirigerVersAccueil() throws Exception {
        Reservation reservation = reservationRepository.save(new Reservation(
                "RES-TESTANNULATION",
                seanceTest,
                "marc.durand@test.fr",
                4,
                StatutReservation.CONFIRMEE,
                LocalDateTime.now()
        ));

        mockMvc.perform(post("/annuler/" + reservation.getCode()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        Reservation reservationApres = reservationRepository.findByCode(reservation.getCode()).orElseThrow();
        assertThat(reservationApres.getStatut()).isEqualTo(StatutReservation.ANNULEE);
    }

    @Test
    @DisplayName("GET /admin/tableau-de-bord : sans authentification, redirige vers la page de connexion")
    void afficherTableauDeBord_anonyme_devraitRedirigerVersConnexion() throws Exception {
        MvcResult resultat = mockMvc.perform(get("/admin/tableau-de-bord"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(resultat.getResponse().getRedirectedUrl()).endsWith("/connexion");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /admin/tableau-de-bord : avec rôle ADMIN, affiche le tableau de bord avec statut 200")
    void afficherTableauDeBord_admin_devraitRenvoyerVueTableau() throws Exception {
        mockMvc.perform(get("/admin/tableau-de-bord"))
                .andExpect(status().isOk())
                .andExpect(view().name("tableau-de-bord"))
                .andExpect(model().attributeExists("tableau"));
    }

    @Test
    @WithMockUser(username = "programmateur", roles = {"PROGRAMMATEUR"})
    @DisplayName("GET /programmation : avec rôle PROGRAMMATEUR, affiche la gestion de la programmation")
    void afficherProgrammation_programmateur_devraitRenvoyerVue() throws Exception {
        mockMvc.perform(get("/programmation"))
                .andExpect(status().isOk())
                .andExpect(view().name("gestion-programmation"))
                .andExpect(model().attributeExists("films", "salles", "seances"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("GET /admin/salles : avec rôle ADMIN, affiche la gestion des salles physiques")
    void afficherGestionSalles_admin_devraitRenvoyerVue() throws Exception {
        mockMvc.perform(get("/admin/salles"))
                .andExpect(status().isOk())
                .andExpect(view().name("gestion-salles"))
                .andExpect(model().attributeExists("salles"));
    }
}
