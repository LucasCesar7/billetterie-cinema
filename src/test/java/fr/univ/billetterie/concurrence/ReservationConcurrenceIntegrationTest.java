package fr.univ.billetterie.concurrence;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ReservationConcurrenceIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private SalleRepository salleRepository;

    @Autowired
    private SeanceRepository seanceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private String getAuthHeaderClient() {
        return "Basic " + Base64.getEncoder().encodeToString("client:client123".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Recette A8 - Scénario nominal de concurrence : 5 places restantes, 2 requêtes simultanées de 4 places (1 succès 201, 1 refus 409)")
    void testConcurrenceDeuxReservationsSimultaneesPourCinqPlacesRestantes() throws Exception {
        // 1. Préparation des données de test
        Film film = filmRepository.save(new Film(
                "Film Concurrence Scénario 1",
                "Science-Fiction",
                135,
                LocalDate.now().plusMonths(1)
        ));

        // Salle de capacité 20 (respecte la contrainte SQL chk_salle_capacite 20..500)
        Salle salle = salleRepository.save(new Salle(
                "Salle Concurrence A8 - " + UUID.randomUUID(),
                20
        ));

        Seance seance = seanceRepository.save(new Seance(
                film,
                salle,
                LocalDateTime.now().plusDays(2),
                new BigDecimal("11.50")
        ));

        // Pré-réservation de 15 places (8 + 7) pour laisser exactement 5 places restantes
        reservationRepository.save(new Reservation(
                "RES-INIT1-" + UUID.randomUUID().toString().substring(0, 8),
                seance,
                "client.initial1@cinema.fr",
                8,
                StatutReservation.CONFIRMEE,
                LocalDateTime.now().minusHours(2)
        ));
        reservationRepository.save(new Reservation(
                "RES-INIT2-" + UUID.randomUUID().toString().substring(0, 8),
                seance,
                "client.initial2@cinema.fr",
                7,
                StatutReservation.CONFIRMEE,
                LocalDateTime.now().minusHours(1)
        ));

        int placesInitialesReservees = reservationRepository.sumPlacesReservees(seance.getId(), StatutReservation.CONFIRMEE);
        assertThat(placesInitialesReservees).isEqualTo(15);
        int placesRestantesAvantTest = salle.getCapacite() - placesInitialesReservees;
        assertThat(placesRestantesAvantTest).isEqualTo(5);

        // 2. Lancement de 2 threads concurrents déclenchés simultanément
        int nombreThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(nombreThreads);
        CountDownLatch pretLatch = new CountDownLatch(nombreThreads);
        CountDownLatch departLatch = new CountDownLatch(1);
        CountDownLatch finLatch = new CountDownLatch(nombreThreads);
        List<HttpResponse<String>> reponses = Collections.synchronizedList(new ArrayList<>());
        HttpClient clientHttp = HttpClient.newHttpClient();

        for (int i = 0; i < nombreThreads; i++) {
            final int indexThread = i;
            executor.submit(() -> {
                try {
                    String payload = """
                            {
                              "seanceId": %d,
                              "emailClient": "concurrent.%d@cinema.fr",
                              "nombrePlaces": 4
                            }
                            """.formatted(seance.getId(), indexThread);

                    HttpRequest requete = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:" + port + "/api/reservations"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", getAuthHeaderClient())
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .build();

                    pretLatch.countDown();
                    departLatch.await(); // Attente du signal de départ commun

                    HttpResponse<String> reponse = clientHttp.send(requete, HttpResponse.BodyHandlers.ofString());
                    reponses.add(reponse);
                } catch (Exception e) {
                    // Erreur inattendue
                } finally {
                    finLatch.countDown();
                }
            });
        }

        // Synchronisation du départ simultané
        assertThat(pretLatch.await(10, TimeUnit.SECONDS)).isTrue();
        departLatch.countDown(); // TOP DÉPART SIMULTANÉ
        assertThat(finLatch.await(15, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // 3. Assertions strictes de la Recette A8
        assertThat(reponses).hasSize(2);

        long nombreSucces = reponses.stream()
                .filter(r -> r.statusCode() == 201)
                .count();

        long nombreConflits = reponses.stream()
                .filter(r -> r.statusCode() == 409
                        && r.body() != null
                        && r.body().contains("CAPACITE_INSUFFISANTE"))
                .count();

        // Exactement une requête reçoit le statut HTTP 201 (succès)
        assertThat(nombreSucces).isEqualTo(1);

        // Exactement une requête reçoit le statut HTTP 409 avec le code CAPACITE_INSUFFISANTE
        assertThat(nombreConflits).isEqualTo(1);

        // La somme des places confirmées en base après exécution des threads est égale à 19 (15 initiales + 4),
        // c'est-à-dire exactement 4 places attribuées par les threads (et JAMAIS 8 places).
        int totalPlacesEnBase = reservationRepository.sumPlacesReservees(seance.getId(), StatutReservation.CONFIRMEE);
        int placesAlloueesParThreads = totalPlacesEnBase - placesInitialesReservees;

        assertThat(placesAlloueesParThreads).isEqualTo(4);
        assertThat(totalPlacesEnBase).isEqualTo(19);
        assertThat(totalPlacesEnBase).isNotEqualTo(23);
        assertThat(totalPlacesEnBase).isLessThanOrEqualTo(salle.getCapacite());
    }

    @Test
    @DisplayName("Recette A8 - Test de stress concurrentiel : 10 places restantes, 10 requêtes simultanées de 2 places (5 succès 201, 5 refus 409)")
    void testStressConcurrenceDixReservationsSimultaneesPourDixPlacesRestantes() throws Exception {
        // 1. Préparation des données de test
        Film film = filmRepository.save(new Film(
                "Film Stress Concurrence",
                "Action",
                140,
                LocalDate.now().plusMonths(2)
        ));

        Salle salle = salleRepository.save(new Salle(
                "Salle Stress A8 - " + UUID.randomUUID(),
                20
        ));

        Seance seance = seanceRepository.save(new Seance(
                film,
                salle,
                LocalDateTime.now().plusDays(3),
                new BigDecimal("14.00")
        ));

        // Pré-réservation de 10 places (5 + 5) pour laisser exactement 10 places disponibles
        reservationRepository.save(new Reservation(
                "RES-STRESS-INIT1-" + UUID.randomUUID().toString().substring(0, 8),
                seance,
                "stress.pre1@cinema.fr",
                5,
                StatutReservation.CONFIRMEE,
                LocalDateTime.now().minusHours(2)
        ));
        reservationRepository.save(new Reservation(
                "RES-STRESS-INIT2-" + UUID.randomUUID().toString().substring(0, 8),
                seance,
                "stress.pre2@cinema.fr",
                5,
                StatutReservation.CONFIRMEE,
                LocalDateTime.now().minusHours(1)
        ));

        int placesInitialesReservees = reservationRepository.sumPlacesReservees(seance.getId(), StatutReservation.CONFIRMEE);
        assertThat(placesInitialesReservees).isEqualTo(10);
        int placesRestantesAvantStress = salle.getCapacite() - placesInitialesReservees;
        assertThat(placesRestantesAvantStress).isEqualTo(10);

        // 2. Déclenchement de 10 threads simultanés demandant chacun 2 places (demande totale = 20 places)
        int nombreThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(nombreThreads);
        CountDownLatch pretLatch = new CountDownLatch(nombreThreads);
        CountDownLatch departLatch = new CountDownLatch(1);
        CountDownLatch finLatch = new CountDownLatch(nombreThreads);
        List<HttpResponse<String>> reponses = Collections.synchronizedList(new ArrayList<>());
        HttpClient clientHttp = HttpClient.newHttpClient();

        for (int i = 0; i < nombreThreads; i++) {
            final int indexThread = i;
            executor.submit(() -> {
                try {
                    String payload = """
                            {
                              "seanceId": %d,
                              "emailClient": "stress.client.%d@cinema.fr",
                              "nombrePlaces": 2
                            }
                            """.formatted(seance.getId(), indexThread);

                    HttpRequest requete = HttpRequest.newBuilder()
                            .uri(URI.create("http://localhost:" + port + "/api/reservations"))
                            .header("Content-Type", "application/json")
                            .header("Authorization", getAuthHeaderClient())
                            .POST(HttpRequest.BodyPublishers.ofString(payload))
                            .build();

                    pretLatch.countDown();
                    departLatch.await(); // Attente de la libération simultanée

                    HttpResponse<String> reponse = clientHttp.send(requete, HttpResponse.BodyHandlers.ofString());
                    reponses.add(reponse);
                } catch (Exception e) {
                    // Erreur inattendue
                } finally {
                    finLatch.countDown();
                }
            });
        }

        // Synchronisation du départ simultané des 10 threads
        assertThat(pretLatch.await(10, TimeUnit.SECONDS)).isTrue();
        departLatch.countDown(); // TOP DÉPART SIMULTANÉ POUR LES 10 THREADS
        assertThat(finLatch.await(20, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // 3. Assertions strictes
        assertThat(reponses).hasSize(10);

        long nombreSucces = reponses.stream()
                .filter(r -> r.statusCode() == 201)
                .count();

        long nombreConflits = reponses.stream()
                .filter(r -> r.statusCode() == 409
                        && r.body() != null
                        && r.body().contains("CAPACITE_INSUFFISANTE"))
                .count();

        // Exactement 5 réservations réussissent (201)
        assertThat(nombreSucces).isEqualTo(5);

        // Exactement 5 réservations échouent (409)
        assertThat(nombreConflits).isEqualTo(5);

        // Le cumul final en base est exactement de 20 places (10 initiales + 10 allouées par les 5 threads gagnants).
        // Capacité maximale respectée à 100%, aucune surréservation.
        int totalPlacesEnBase = reservationRepository.sumPlacesReservees(seance.getId(), StatutReservation.CONFIRMEE);
        int placesAlloueesParThreads = totalPlacesEnBase - placesInitialesReservees;

        assertThat(placesAlloueesParThreads).isEqualTo(10);
        assertThat(totalPlacesEnBase).isEqualTo(20);
        assertThat(totalPlacesEnBase).isEqualTo(salle.getCapacite());
    }
}
