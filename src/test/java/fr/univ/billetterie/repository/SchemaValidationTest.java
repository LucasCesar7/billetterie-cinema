package fr.univ.billetterie.repository;

import static org.assertj.core.api.Assertions.assertThat;

import fr.univ.billetterie.entity.Film;
import fr.univ.billetterie.entity.Reservation;
import fr.univ.billetterie.entity.Salle;
import fr.univ.billetterie.entity.Seance;
import fr.univ.billetterie.entity.StatutReservation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class SchemaValidationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("billetterie_db")
            .withUsername("cinema_user")
            .withPassword("cinema_pass");

    @Autowired
    private FilmRepository filmRepository;

    @Autowired
    private SalleRepository salleRepository;

    @Autowired
    private SeanceRepository seanceRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("Validation du schéma : les migrations Flyway et les entités JPA sont conformes")
    void validerMigrationsEtRepositories() {
        // 1. Vérification des données initiales insérées par Flyway V2 (Films)
        List<Film> films = filmRepository.findAll();
        assertThat(films).hasSizeGreaterThanOrEqualTo(2);
        assertThat(filmRepository.findByTitreContainingIgnoreCase("Inception")).isNotEmpty();
        assertThat(filmRepository.findByGenreContainingIgnoreCase("Science-Fiction")).hasSizeGreaterThanOrEqualTo(2);

        // 2. Vérification de la salle de démonstration
        Optional<Salle> salleOptionnelle = salleRepository.findByNom("Salle 1");
        assertThat(salleOptionnelle).isPresent();
        assertThat(salleOptionnelle.get().getCapacite()).isEqualTo(100);
        assertThat(salleRepository.existsByNom("Salle 1")).isTrue();

        // 3. Vérification de la séance de démonstration
        LocalDateTime debutPlage = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime finPlage = LocalDateTime.of(2026, 12, 31, 23, 59);
        List<Seance> seances = seanceRepository.findByDebutBetween(debutPlage, finPlage);
        assertThat(seances).isNotEmpty();

        Seance premiereSeance = seances.getFirst();
        assertThat(premiereSeance.getPrixUnitaire()).isEqualByComparingTo(new BigDecimal("10.50"));
    }

    @Test
    @Transactional
    @DisplayName("Vérification du verrou pessimiste et des opérations de réservation")
    void validerVerrouEtReservation() {
        // Récupération de la séance créée par V2
        List<Seance> seances = seanceRepository.findAll();
        assertThat(seances).isNotEmpty();
        Seance seance = seances.getFirst();

        // Test du verrouillage pessimiste
        Optional<Seance> seanceVerrouillee = seanceRepository.findByIdWithLock(seance.getId());
        assertThat(seanceVerrouillee).isPresent();

        // Création d'une réservation de test
        String codeReservation = "RES-TEST-001";
        Reservation reservation = new Reservation(
                codeReservation,
                seance,
                "etudiant@example.test",
                3,
                StatutReservation.CONFIRMEE,
                LocalDateTime.now()
        );

        reservationRepository.save(reservation);

        // Vérification de la réservation et de la somme des places
        assertThat(reservationRepository.existsByCode(codeReservation)).isTrue();
        Optional<Reservation> reservationTrouvee = reservationRepository.findByCode(codeReservation);
        assertThat(reservationTrouvee).isPresent();
        assertThat(reservationTrouvee.get().getNombrePlaces()).isEqualTo(3);

        int totalPlaces = reservationRepository.sumPlacesReservees(seance.getId(), StatutReservation.CONFIRMEE);
        assertThat(totalPlaces).isEqualTo(3);

        int totalAnnulees = reservationRepository.sumPlacesReservees(seance.getId(), StatutReservation.ANNULEE);
        assertThat(totalAnnulees).isEqualTo(0);
    }
}

