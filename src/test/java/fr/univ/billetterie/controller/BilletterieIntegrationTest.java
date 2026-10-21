package fr.univ.billetterie.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.univ.billetterie.dto.ReservationCreationRequest;
import fr.univ.billetterie.dto.SalleCreationRequest;
import fr.univ.billetterie.dto.SeanceCreationRequest;
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
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$[0].titre").exists())
                .andExpect(jsonPath("$[0].dureeMinutes").exists());

        // Filtrage par titre
        mockMvc.perform(get("/api/films").param("titre", "Inception"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titre").value("Inception"));
    }

    @Test
    @DisplayName("Recette A4 : validation échouée sur réservation (0 ou 9 places -> 400 VALIDATION_ECHOUEE)")
    void recetteA4_validationEchoueeReservation() throws Exception {
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
    @DisplayName("Recette A6 & A7 : réservation valide (201) et refus pour capacité insuffisante (409)")
    void recetteA6EtA7_reservationValideEtCapaciteInsuffisante() throws Exception {
        // Préparation d'une salle à capacité réduite (20 places) et d'une séance dédiée
        Salle petiteSalle = salleRepository.save(new Salle("Salle Petite", 20));
        Film film = filmRepository.findAll().getFirst();
        Seance seance = seanceRepository.save(new Seance(
                film,
                petiteSalle,
                LocalDateTime.of(2026, 12, 1, 14, 0),
                new BigDecimal("12.00")
        ));

        // Réservation valide de 8 places
        ReservationCreationRequest requeteValide = new ReservationCreationRequest(
                seance.getId(),
                "lea@example.test",
                8
        );

        mockMvc.perform(post("/api/reservations")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteValide)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.statut").value("CONFIRMEE"))
                .andExpect(jsonPath("$.nombrePlaces").value(8))
                .andExpect(jsonPath("$.montantTotal").value(96.00)) // 8 * 12.00
                .andExpect(jsonPath("$.seanceId").value(seance.getId()));

        // Deuxième réservation de 8 places (reste 20 - 8 = 12 places -> 8 places acceptées)
        ReservationCreationRequest requeteValide2 = new ReservationCreationRequest(
                seance.getId(),
                "paul@example.test",
                8
        );
        mockMvc.perform(post("/api/reservations")
                        .with(httpBasic("client", "client123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requeteValide2)))
                .andExpect(status().isCreated());

        // Il reste 20 - 16 = 4 places. Une demande de 5 places (invalide pour la capacité) doit être rejetée avec 409
        ReservationCreationRequest requeteTropDePlaces = new ReservationCreationRequest(
                seance.getId(),
                "marc@example.test",
                5
        );

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
}
