package fr.univ.billetterie.repository;

import fr.univ.billetterie.entity.Salle;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalleRepository extends JpaRepository<Salle, Long> {

    Optional<Salle> findByNom(String nom);

    boolean existsByNom(String nom);
}

