package fr.univ.billetterie.repository;

import fr.univ.billetterie.entity.Film;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilmRepository extends JpaRepository<Film, Long> {

    List<Film> findByTitreContainingIgnoreCaseOrGenreContainingIgnoreCase(String titre, String genre);

    List<Film> findByTitreContainingIgnoreCase(String titre);

    List<Film> findByGenreContainingIgnoreCase(String genre);
}

