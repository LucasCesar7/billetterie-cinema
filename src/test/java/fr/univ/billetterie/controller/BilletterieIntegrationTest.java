package fr.univ.billetterie.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.univ.billetterie.dto.FilmCreationRequest;
import fr.univ.billetterie.dto.FilmModificationRequest;
import fr.univ.billetterie.dto.ReservationCreationRequest;
import fr.univ.billetterie.dto.SalleCreationRequest;
import fr.univ.billetterie.dto.SalleModificationRequest;
import fr.univ.billetterie.dto.SeanceCreationRequest;
import fr.univ.billetterie.dto.SeanceModificationRequest;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BilletterieIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("billetterie_db")
            .withUsername("cinema_user")
            .withPassword("cinema_pass");

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private SalleRepository salleRepository;

    @Autowired
    private SeanceRepository seanceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("Recette A3 : consultation publique des films par un appel anonyme")
    void recetteA3_consultationPubliqueFilms() throws Exception {
        mockMvc.perform(get("/api/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].titre").isNotEmpty())
                .andExpect(jsonPath("$[0].dureeMinutes").isNumber());
    }

    @Test
    @DisplayName("Recette A4 : validation stricte d'un film (rejet 400 avec code VALIDATION_ECHOUEE)")
    void recetteA4_validationEntreesFilmInvalide() throws Exception {
        // Durée < 40 minutes -> rejet
        FilmCreationRequest requeteDureeTropCourte = new FilmCreationRequest(
                "Court métrage",
                "Animation",
                15,
                LocalDate.of(2026, 5, 1)
        );

        mockMvc.perform(post("/api/programmation/films")
                        .with(httpBasic("programmateur", "prog123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteDureeTropCourte)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"))
                .andExpect(jsonPath("$.path").value("/api/programmation/films"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        // Titre vide -> rejet
        FilmCreationRequest requeteTitreVide = new FilmCreationRequest(
                "   ",
                "Comédie",
                90,
                LocalDate.of(2026, 5, 1)
        );

        mockMvc.perform(post("/api/programmation/films")
                        .with(httpBasic("programmateur", "prog123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteTitreVide)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"));
    }

    @Test
    @DisplayName("Recette A4 (suite) : validation de la réservation (rejet si nombrePlaces < 1 ou > 8)")
    void recetteA4_validationReservationNombrePlaces() throws Exception {
        ReservationCreationRequest requeteZeroPlace = new ReservationCreationRequest(
                1L,
                "client@example.test",
                0
        );

        mockMvc.perform(post("/api/reservations")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteZeroPlace)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"))
                .andExpect(jsonPath("$.path").value("/api/reservations"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        ReservationCreationRequest requeteNeufPlaces = new ReservationCreationRequest(
                1L,
                "client@example.test",
                9
        );

        mockMvc.perform(post("/api/reservations")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteNeufPlaces)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ECHOUEE"))
                .andExpect(jsonPath("$.path").value("/api/reservations"));
    }

    @Test
    @DisplayName("Recette A5 : sécurité des routes (CLIENT refusé avec 403, anonyme avec 401)")
    void recetteA5_securiteDesRoutes() throws Exception {
        SalleCreationRequest requeteSalle = new SalleCreationRequest("Salle VIP", 50);

        // Appel anonyme -> 401 Unauthorized
        mockMvc.perform(post("/api/admin/salles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteSalle)))
                .andExpect(status().isUnauthorized());

        // Appel par CLIENT -> 403 Forbidden
        mockMvc.perform(post("/api/admin/salles")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteSalle)))
                .andExpect(status().isForbidden());

        // Appel par ADMIN -> 201 Created
        mockMvc.perform(post("/api/admin/salles")
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteSalle)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.nom").value("Salle VIP"))
                .andExpect(jsonPath("$.capacite").value(50));
    }

    @Test
    @DisplayName("Recette A6 : réservation nominale par un client authentifié (calcul exact du montant)")
    void recetteA6_reservationNominale() throws Exception {
        // Préparation d'une séance de test
        Film film = filmRepository.findAll().getFirst();
        Salle salle = salleRepository.save(new Salle("Salle 50 Places", 50));
        Seance seance = seanceRepository.save(new Seance(
                film,
                salle,
                LocalDateTime.of(2026, 11, 20, 20, 30),
                new BigDecimal("12.50")
        ));

        ReservationCreationRequest requete = new ReservationCreationRequest(
                seance.getId(),
                "jean.dupont@test.univ",
                4
        );

        mockMvc.perform(post("/api/reservations")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.statut").value("CONFIRMEE"))
                .andExpect(jsonPath("$.nombrePlaces").value(4))
                .andExpect(jsonPath("$.montantTotal").value(50.00)) // 4 * 12.50 = 50.00
                .andExpect(jsonPath("$.seanceId").value(seance.getId()));
    }

    @Test
    @DisplayName("Recette A7 : refus de surréservation (HTTP 409 et CAPACITE_INSUFFISANTE)")
    void recetteA7_refusSurreservation() throws Exception {
        Salle petiteSalle = salleRepository.save(new Salle("Petite Salle", 20));
        Film film = filmRepository.findAll().getFirst();
        Seance seance = seanceRepository.save(new Seance(
                film,
                petiteSalle,
                LocalDateTime.of(2026, 11, 25, 14, 0),
                new BigDecimal("8.00")
        ));

        // Réservation de 16 places (2 requêtes de 8)
        ReservationCreationRequest requete8 = new ReservationCreationRequest(seance.getId(), "a@test.univ", 8);
        mockMvc.perform(post("/api/reservations")
                .with(httpBasic("client", "client123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete8))).andExpect(status().isCreated());

        mockMvc.perform(post("/api/reservations")
                .with(httpBasic("client", "client123"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requete8))).andExpect(status().isCreated());

        // Capacité occupée : 16/20. Reste 4 places.
        // Tentative de réserver 5 places -> Refus 409
        ReservationCreationRequest requeteTropDePlaces = new ReservationCreationRequest(seance.getId(), "b@test.univ", 5);

        mockMvc.perform(post("/api/reservations")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteTropDePlaces)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CAPACITE_INSUFFISANTE"))
                .andExpect(jsonPath("$.message").value("Il ne reste que 4 places pour cette séance"))
                .andExpect(jsonPath("$.path").value("/api/reservations"));
    }

    @Test
    @DisplayName("Recette A9 : annulation répondant 204 et libérant les places")
    void recetteA9_annulationReservationLiberePlaces() throws Exception {
        Salle salle = salleRepository.save(new Salle("Salle Annulation", 20));
        Film film = filmRepository.findAll().getFirst();
        Seance seance = seanceRepository.save(new Seance(
                film,
                salle,
                LocalDateTime.of(2026, 12, 10, 18, 0),
                new BigDecimal("10.00")
        ));

        // Création d'une réservation de 3 places
        ReservationCreationRequest requete = new ReservationCreationRequest(
                seance.getId(),
                "annul@example.test",
                3
        );

        MvcResult resultat = mockMvc.perform(post("/api/reservations")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andReturn();

        String reponseJson = resultat.getResponse().getContentAsString();
        String code = objectMapper.readTree(reponseJson).get("code").asText();

        int placesAvantAnnulation = reservationRepository.sumPlacesReservees(seance.getId(), StatutReservation.CONFIRMEE);
        assertThat(placesAvantAnnulation).isEqualTo(3);

        // Annulation par CLIENT -> 204 No Content
        mockMvc.perform(delete("/api/reservations/" + code)
                        .with(httpBasic("client", "client123")))
                .andExpect(status().isNoContent());

        // Vérification de la libération des places
        int placesApresAnnulation = reservationRepository.sumPlacesReservees(seance.getId(), StatutReservation.CONFIRMEE);
        assertThat(placesApresAnnulation).isEqualTo(0);

        Reservation reservationAnnulee = reservationRepository.findByCode(code).orElseThrow();
        assertThat(reservationAnnulee.getStatut()).isEqualTo(StatutReservation.ANNULEE);
    }

    @Test
    @DisplayName("Phase 8 CRUD : modification valide d'un film (PUT 200)")
    void recetteCrud_modifierFilmValide() throws Exception {
        Film film = filmRepository.save(new Film("Film Initial", "Action", 120, LocalDate.of(2025, 1, 1)));

        FilmModificationRequest requete = new FilmModificationRequest(
                "Film Modifie",
                "Aventure",
                140,
                LocalDate.of(2025, 6, 1)
        );

        mockMvc.perform(put("/api/programmation/films/" + film.getId())
                        .with(httpBasic("programmateur", "prog123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(film.getId()))
                .andExpect(jsonPath("$.titre").value("Film Modifie"))
                .andExpect(jsonPath("$.genre").value("Aventure"))
                .andExpect(jsonPath("$.dureeMinutes").value(140));
    }

    @Test
    @DisplayName("Phase 8 CRUD : suppression réussie d'un film sans séance (DELETE 204)")
    void recetteCrud_supprimerFilmSansSeance() throws Exception {
        Film film = filmRepository.save(new Film("Film Isole", "Drame", 95, LocalDate.of(2025, 3, 1)));

        mockMvc.perform(delete("/api/programmation/films/" + film.getId())
                        .with(httpBasic("programmateur", "prog123")))
                .andExpect(status().isNoContent());

        assertThat(filmRepository.existsById(film.getId())).isFalse();
    }

    @Test
    @DisplayName("Phase 8 CRUD : refus de suppression d'une salle avec séances associées (DELETE 409)")
    void recetteCrud_refusSuppressionSalleAvecSeances() throws Exception {
        Salle salle = salleRepository.save(new Salle("Salle Avec Seances", 40));
        Film film = filmRepository.findAll().getFirst();
        seanceRepository.save(new Seance(
                film,
                salle,
                LocalDateTime.of(2026, 12, 15, 21, 0),
                new BigDecimal("11.00")
        ));

        mockMvc.perform(delete("/api/admin/salles/" + salle.getId())
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("IMPOSSIBLE_SUPPRIMER_SALLE"));

        assertThat(salleRepository.existsById(salle.getId())).isTrue();
    }

    @Test
    @DisplayName("Phase 8 CRUD : modification valide d'une salle physique (PUT 200)")
    void recetteCrud_modifierSalleValide() throws Exception {
        Salle salle = salleRepository.save(new Salle("Salle A Renommer", 35));

        SalleModificationRequest requete = new SalleModificationRequest("Salle Renommee", 55);

        mockMvc.perform(put("/api/admin/salles/" + salle.getId())
                        .with(httpBasic("admin", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(salle.getId()))
                .andExpect(jsonPath("$.nom").value("Salle Renommee"))
                .andExpect(jsonPath("$.capacite").value(55));

        Salle salleEnBase = salleRepository.findById(salle.getId()).orElseThrow();
        assertThat(salleEnBase.getNom()).isEqualTo("Salle Renommee");
        assertThat(salleEnBase.getCapacite()).isEqualTo(55);
    }

    @Test
    @DisplayName("Phase 8 CRUD : suppression réussie d'une salle sans séance (DELETE 204)")
    void recetteCrud_supprimerSalleSansSeance() throws Exception {
        Salle salle = salleRepository.save(new Salle("Salle A Supprimer", 25));

        mockMvc.perform(delete("/api/admin/salles/" + salle.getId())
                        .with(httpBasic("admin", "admin123")))
                .andExpect(status().isNoContent());

        assertThat(salleRepository.existsById(salle.getId())).isFalse();
    }

    @Test
    @DisplayName("Phase 8 CRUD : modification valide d'une séance (PUT 200)")
    void recetteCrud_modifierSeanceValide() throws Exception {
        Salle salle = salleRepository.save(new Salle("Salle Seance Modif", 60));
        Film film = filmRepository.findAll().getFirst();
        Seance seance = seanceRepository.save(new Seance(
                film,
                salle,
                LocalDateTime.of(2026, 12, 20, 15, 0),
                new BigDecimal("10.00")
        ));

        LocalDateTime nouvelleDate = LocalDateTime.of(2026, 12, 21, 16, 30);
        SeanceModificationRequest requete = new SeanceModificationRequest(
                film.getId(),
                salle.getId(),
                nouvelleDate,
                new BigDecimal("13.50")
        );

        mockMvc.perform(put("/api/programmation/seances/" + seance.getId())
                        .with(httpBasic("programmateur", "prog123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(seance.getId()))
                .andExpect(jsonPath("$.prixUnitaire").value(13.50));

        Seance seanceEnBase = seanceRepository.findById(seance.getId()).orElseThrow();
        assertThat(seanceEnBase.getDebut()).isEqualTo(nouvelleDate);
        assertThat(seanceEnBase.getPrixUnitaire()).isEqualByComparingTo("13.50");
    }

    @Test
    @DisplayName("Phase 8 CRUD : suppression réussie d'une séance sans réservation (DELETE 204)")
    void recetteCrud_supprimerSeanceSansReservation() throws Exception {
        Salle salle = salleRepository.save(new Salle("Salle Seance Libree", 45));
        Film film = filmRepository.findAll().getFirst();
        Seance seance = seanceRepository.save(new Seance(
                film,
                salle,
                LocalDateTime.of(2026, 12, 22, 19, 0),
                new BigDecimal("10.50")
        ));

        mockMvc.perform(delete("/api/programmation/seances/" + seance.getId())
                        .with(httpBasic("programmateur", "prog123")))
                .andExpect(status().isNoContent());

        assertThat(seanceRepository.existsById(seance.getId())).isFalse();
    }

    @Test
    @DisplayName("Phase 8 CRUD : refus de suppression d'une séance avec réservations actives (DELETE 409)")
    void recetteCrud_refusSuppressionSeanceAvecReservations() throws Exception {
        Salle salle = salleRepository.save(new Salle("Salle Seance Bloquee", 45));
        Film film = filmRepository.findAll().getFirst();
        Seance seance = seanceRepository.save(new Seance(
                film,
                salle,
                LocalDateTime.of(2026, 12, 23, 20, 0),
                new BigDecimal("12.00")
        ));

        reservationRepository.save(new Reservation(
                "RES-BLOQUER-CRUD",
                seance,
                "client.crud@test.univ",
                2,
                StatutReservation.CONFIRMEE,
                LocalDateTime.now()
        ));

        mockMvc.perform(delete("/api/programmation/seances/" + seance.getId())
                        .with(httpBasic("programmateur", "prog123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("IMPOSSIBLE_SUPPRIMER_SEANCE"));

        assertThat(seanceRepository.existsById(seance.getId())).isTrue();
    }
}
