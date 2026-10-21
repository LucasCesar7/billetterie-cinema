package fr.univ.billetterie.service;

import fr.univ.billetterie.dto.SalleCreationRequest;
import fr.univ.billetterie.dto.SalleModificationRequest;
import fr.univ.billetterie.dto.SalleResponse;
import fr.univ.billetterie.entity.Salle;
import fr.univ.billetterie.exception.ConflitMetierException;
import fr.univ.billetterie.exception.RessourceIntrouvableException;
import fr.univ.billetterie.repository.SalleRepository;
import fr.univ.billetterie.repository.SeanceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalleService {

    private final SalleRepository salleRepository;
    private final SeanceRepository seanceRepository;

    public SalleService(SalleRepository salleRepository, SeanceRepository seanceRepository) {
        this.salleRepository = salleRepository;
        this.seanceRepository = seanceRepository;
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

    @Transactional
    public SalleResponse modifierSalle(Long id, SalleModificationRequest requete) {
        Salle salle = salleRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "SALLE_INTROUVABLE",
                        "La salle avec l'identifiant " + id + " n'existe pas"
                ));

        if (salleRepository.existsByNomAndIdNot(requete.nom(), id)) {
            throw new ConflitMetierException(
                    "NOM_SALLE_EXISTANT",
                    "Une salle avec le nom '" + requete.nom() + "' existe déjà"
            );
        }

        salle.setNom(requete.nom());
        salle.setCapacite(requete.capacite());

        Salle salleMisAJour = salleRepository.save(salle);
        return versResponse(salleMisAJour);
    }

    @Transactional
    public void supprimerSalle(Long id) {
        Salle salle = salleRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "SALLE_INTROUVABLE",
                        "La salle avec l'identifiant " + id + " n'existe pas"
                ));

        if (seanceRepository.existsBySalleId(id)) {
            throw new ConflitMetierException(
                    "IMPOSSIBLE_SUPPRIMER_SALLE",
                    "Impossible de supprimer cette salle : des séances y sont programmées"
            );
        }

        salleRepository.delete(salle);
    }

    @Transactional(readOnly = true)
    public Salle trouverSalle(Long id) {
        return salleRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException(
                        "SALLE_INTROUVABLE",
                        "La salle avec l'identifiant " + id + " n'existe pas"
                ));
    }

    @Transactional(readOnly = true)
    public List<Salle> listerToutesLesSalles() {
        return salleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public long compterSeancesPourSalle(Long salleId) {
        return seanceRepository.countBySalleId(salleId);
    }

    public SalleResponse versResponse(Salle salle) {
        return new SalleResponse(
                salle.getId(),
                salle.getNom(),
                salle.getCapacite()
        );
    }
}
