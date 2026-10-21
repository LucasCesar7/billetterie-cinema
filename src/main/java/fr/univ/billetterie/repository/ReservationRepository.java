package fr.univ.billetterie.repository;

import fr.univ.billetterie.entity.Reservation;
import fr.univ.billetterie.entity.StatutReservation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByCode(String code);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.seance s JOIN FETCH s.film JOIN FETCH s.salle WHERE r.code = :code")
    Optional<Reservation> findByCodeWithSeanceAndFilmAndSalle(@Param("code") String code);

    boolean existsByCode(String code);

    @Query("SELECT COALESCE(SUM(r.nombrePlaces), 0) FROM Reservation r WHERE r.seance.id = :seanceId AND r.statut = :statut")
    int sumPlacesReservees(@Param("seanceId") Long seanceId, @Param("statut") StatutReservation statut);
}

