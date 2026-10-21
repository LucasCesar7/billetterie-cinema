package fr.univ.billetterie.service;

import fr.univ.billetterie.dto.SalleCreationRequest;
import fr.univ.billetterie.dto.SalleResponse;
import fr.univ.billetterie.entity.Salle;
import fr.univ.billetterie.exception.ConflitMetierException;
import fr.univ.billetterie.repository.SalleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalleService {

    private final SalleRepository salleRepository;

    public SalleService(SalleRepository salleRepository) {
        this.salleRepository = salleRepository;
    }

    @Transactional
    public SalleResponse creerSalle(SalleCreationRequest requete) {
        if (salleRepository.existsByNom(requete.nom())) {
            throw new ConflitMetierException("NOM_SALLE_EXISTANT", "Une salle avec le nom '" + requete.nom() + "' existe déjà");
        }

        Salle salle = new Salle(requete.nom(), requete.capacite());
        Salle salleEnregistree = salleRepository.save(salle);
        return versResponse(salleEnregistree);
    }

    public SalleResponse versResponse(Salle salle) {
        return new SalleResponse(
                salle.getId(),
                salle.getNom(),
                salle.getCapacite()
        );
    }
}

