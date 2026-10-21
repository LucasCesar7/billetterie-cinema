package fr.univ.billetterie.repository;

import fr.univ.billetterie.entity.Seance;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SeanceRepository extends JpaRepository<Seance, Long> {

    List<Seance> findByDebutBetween(LocalDateTime du, LocalDateTime au);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seance s WHERE s.id = :id")
    Optional<Seance> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT s FROM Seance s JOIN FETCH s.film JOIN FETCH s.salle ORDER BY s.debut ASC")
    List<Seance> findAllWithFilmAndSalle();

    @Query("SELECT s FROM Seance s JOIN FETCH s.film JOIN FETCH s.salle WHERE s.id = :id")
    Optional<Seance> findByIdWithFilmAndSalle(@Param("id") Long id);

    @Query("SELECT s FROM Seance s JOIN FETCH s.film JOIN FETCH s.salle WHERE s.film.id = :filmId ORDER BY s.debut ASC")
    List<Seance> findByFilmIdWithFilmAndSalle(@Param("filmId") Long filmId);

    boolean existsByFilmId(Long filmId);

    boolean existsBySalleId(Long salleId);

    long countByFilmId(Long filmId);

    long countBySalleId(Long salleId);
}
