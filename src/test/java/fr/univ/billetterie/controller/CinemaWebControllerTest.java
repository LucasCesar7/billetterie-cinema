package fr.univ.billetterie.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
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

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Web Salles : GET /admin/salles/{id}/modifier affiche le formulaire d'édition")
    void afficherFormulaireModifierSalle_admin_devraitRenvoyerVue() throws Exception {
        mockMvc.perform(get("/admin/salles/" + salleTest.getId() + "/modifier"))
                .andExpect(status().isOk())
                .andExpect(view().name("modifier-salle"))
                .andExpect(model().attributeExists("salle"));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Web Salles : POST /admin/salles/{id}/modifier met à jour la salle et redirige")
    void modifierSalleWeb_admin_devraitModifierEtRediriger() throws Exception {
        mockMvc.perform(post("/admin/salles/" + salleTest.getId() + "/modifier")
                        .param("nom", "Salle IMAX Renommee")
                        .param("capacite", "120"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/salles"))
                .andExpect(flash().attributeExists("messageSucces"));

        Salle salleModifiee = salleRepository.findById(salleTest.getId()).orElseThrow();
        assertThat(salleModifiee.getNom()).isEqualTo("Salle IMAX Renommee");
        assertThat(salleModifiee.getCapacite()).isEqualTo(120);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Web Salles : POST /admin/salles/{id}/supprimer refuse la suppression si des séances existent")
    void supprimerSalleWeb_avecSeances_devraitEchouer() throws Exception {
        mockMvc.perform(post("/admin/salles/" + salleTest.getId() + "/supprimer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/salles"))
                .andExpect(flash().attributeExists("messageErreur"));

        assertThat(salleRepository.existsById(salleTest.getId())).isTrue();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Web Salles : POST /admin/salles/{id}/supprimer supprime une salle sans séance")
    void supprimerSalleWeb_sansSeance_devraitSupprimer() throws Exception {
        Salle salleIsolee = salleRepository.save(new Salle("Salle Temporaire", 30));

        mockMvc.perform(post("/admin/salles/" + salleIsolee.getId() + "/supprimer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/salles"))
                .andExpect(flash().attributeExists("messageSucces"));

        assertThat(salleRepository.existsById(salleIsolee.getId())).isFalse();
    }

    @Test
    @WithMockUser(username = "programmateur", roles = {"PROGRAMMATEUR"})
    @DisplayName("Web Films : GET /programmation/films/{id}/modifier affiche le formulaire d'édition")
    void afficherFormulaireModifierFilm_devraitRenvoyerVue() throws Exception {
        mockMvc.perform(get("/programmation/films/" + filmTest.getId() + "/modifier"))
                .andExpect(status().isOk())
                .andExpect(view().name("modifier-film"))
                .andExpect(model().attributeExists("film"));
    }

    @Test
    @WithMockUser(username = "programmateur", roles = {"PROGRAMMATEUR"})
    @DisplayName("Web Films : POST /programmation/films/{id}/modifier modifie le film et redirige")
    void modifierFilmWeb_devraitModifierEtRediriger() throws Exception {
        mockMvc.perform(post("/programmation/films/" + filmTest.getId() + "/modifier")
                        .param("titre", "Dune 2 Version Longue")
                        .param("genre", "Sci-Fi Epique")
                        .param("dureeMinutes", "185")
                        .param("dateSortie", "2024-03-15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/programmation"))
                .andExpect(flash().attributeExists("messageSucces"));

        Film filmModifie = filmRepository.findById(filmTest.getId()).orElseThrow();
        assertThat(filmModifie.getTitre()).isEqualTo("Dune 2 Version Longue");
        assertThat(filmModifie.getDureeMinutes()).isEqualTo(185);
    }

    @Test
    @WithMockUser(username = "programmateur", roles = {"PROGRAMMATEUR"})
    @DisplayName("Web Films : POST /programmation/films/{id}/supprimer refuse la suppression si des séances existent")
    void supprimerFilmWeb_avecSeances_devraitEchouer() throws Exception {
        mockMvc.perform(post("/programmation/films/" + filmTest.getId() + "/supprimer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/programmation"))
                .andExpect(flash().attributeExists("messageErreur"));

        assertThat(filmRepository.existsById(filmTest.getId())).isTrue();
    }

    @Test
    @WithMockUser(username = "programmateur", roles = {"PROGRAMMATEUR"})
    @DisplayName("Web Films : POST /programmation/films/{id}/supprimer supprime un film sans séance")
    void supprimerFilmWeb_sansSeance_devraitSupprimer() throws Exception {
        Film filmIsole = filmRepository.save(new Film("Film Isole", "Drame", 95, LocalDate.of(2025, 1, 10)));

        mockMvc.perform(post("/programmation/films/" + filmIsole.getId() + "/supprimer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/programmation"))
                .andExpect(flash().attributeExists("messageSucces"));

        assertThat(filmRepository.existsById(filmIsole.getId())).isFalse();
    }

    @Test
    @WithMockUser(username = "programmateur", roles = {"PROGRAMMATEUR"})
    @DisplayName("Web Séances : GET /programmation/seances/{id}/modifier affiche le formulaire d'édition")
    void afficherFormulaireModifierSeance_devraitRenvoyerVue() throws Exception {
        mockMvc.perform(get("/programmation/seances/" + seanceTest.getId() + "/modifier"))
                .andExpect(status().isOk())
                .andExpect(view().name("modifier-seance"))
                .andExpect(model().attributeExists("seance", "films", "salles"));
    }

    @Test
    @WithMockUser(username = "programmateur", roles = {"PROGRAMMATEUR"})
    @DisplayName("Web Séances : POST /programmation/seances/{id}/modifier met à jour la séance et redirige")
    void modifierSeanceWeb_devraitModifierEtRediriger() throws Exception {
        LocalDateTime nouveauDebut = LocalDateTime.now().plusDays(4).withHour(18).withMinute(30).withSecond(0).withNano(0);

        mockMvc.perform(post("/programmation/seances/" + seanceTest.getId() + "/modifier")
                        .param("filmId", filmTest.getId().toString())
                        .param("salleId", salleTest.getId().toString())
                        .param("debut", nouveauDebut.toString())
                        .param("prixUnitaire", "15.50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/programmation"))
                .andExpect(flash().attributeExists("messageSucces"));

        Seance seanceModifiee = seanceRepository.findById(seanceTest.getId()).orElseThrow();
        assertThat(seanceModifiee.getPrixUnitaire()).isEqualByComparingTo("15.50");
    }

    @Test
    @WithMockUser(username = "programmateur", roles = {"PROGRAMMATEUR"})
    @DisplayName("Web Séances : POST /programmation/seances/{id}/supprimer refuse la suppression si des réservations existent")
    void supprimerSeanceWeb_avecReservations_devraitEchouer() throws Exception {
        reservationRepository.save(new Reservation(
                "RES-BLOQUER",
                seanceTest,
                "client@test.fr",
                2,
                StatutReservation.CONFIRMEE,
                LocalDateTime.now()
        ));

        mockMvc.perform(post("/programmation/seances/" + seanceTest.getId() + "/supprimer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/programmation"))
                .andExpect(flash().attributeExists("messageErreur"));

        assertThat(seanceRepository.existsById(seanceTest.getId())).isTrue();
    }

    @Test
    @WithMockUser(username = "programmateur", roles = {"PROGRAMMATEUR"})
    @DisplayName("Web Séances : POST /programmation/seances/{id}/supprimer supprime une séance sans réservation")
    void supprimerSeanceWeb_sansReservation_devraitSupprimer() throws Exception {
        Seance seanceIsolee = seanceRepository.save(new Seance(
                filmTest,
                salleTest,
                LocalDateTime.now().plusDays(5).withHour(14).withMinute(0),
                BigDecimal.valueOf(9.00)
        ));

        mockMvc.perform(post("/programmation/seances/" + seanceIsolee.getId() + "/supprimer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/programmation"))
                .andExpect(flash().attributeExists("messageSucces"));

        assertThat(seanceRepository.existsById(seanceIsolee.getId())).isFalse();
    }
}
