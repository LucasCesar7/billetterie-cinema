package fr.univ.billetterie.controller.web;

import fr.univ.billetterie.dto.ReservationCreationRequest;
import fr.univ.billetterie.dto.ReservationResponse;
import fr.univ.billetterie.dto.web.BilletConfirmationVue;
import fr.univ.billetterie.dto.web.FilmCatalogueVue;
import fr.univ.billetterie.dto.web.SeanceDetailVue;
import fr.univ.billetterie.dto.web.SeanceResumeVue;
import fr.univ.billetterie.dto.web.TableauDeBordVue;
import fr.univ.billetterie.entity.Film;
import fr.univ.billetterie.entity.Reservation;
import fr.univ.billetterie.entity.Seance;
import fr.univ.billetterie.entity.StatutReservation;
import fr.univ.billetterie.exception.CapaciteInsuffisanteException;
import fr.univ.billetterie.service.FilmService;
import fr.univ.billetterie.service.ReservationService;
import fr.univ.billetterie.service.SeanceService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CinemaWebController {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH'h'mm", Locale.FRENCH);
    private static final DateTimeFormatter FORMAT_DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH'h'mm", Locale.FRENCH);

    private final FilmService filmService;
    private final SeanceService seanceService;
    private final ReservationService reservationService;
    private final MeterRegistry meterRegistry;

    public CinemaWebController(
            FilmService filmService,
            SeanceService seanceService,
            ReservationService reservationService,
            MeterRegistry meterRegistry
    ) {
        this.filmService = filmService;
        this.seanceService = seanceService;
        this.reservationService = reservationService;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping({"/", "/films"})
    public String afficherCatalogue(Model model) {
        List<Film> films = filmService.listerTousLesFilms();
        List<FilmCatalogueVue> vuesFilms = new ArrayList<>();

        for (Film film : films) {
            List<Seance> seancesFilm = seanceService.listerSeancesParFilm(film.getId());
            List<SeanceResumeVue> vuesSeances = seancesFilm.stream()
                    .map(this::construireSeanceResume)
                    .toList();

            String dureeFormatee = formaterDuree(film.getDureeMinutes());

            vuesFilms.add(new FilmCatalogueVue(
                    film.getId(),
                    film.getTitre(),
                    film.getGenre(),
                    film.getDureeMinutes(),
                    dureeFormatee,
                    film.getDateSortie(),
                    vuesSeances
            ));
        }

        model.addAttribute("films", vuesFilms);
        return "index";
    }

    @GetMapping("/seances/{id}/reserver")
    public String afficherPageReservation(@PathVariable Long id, Model model) {
        Seance seance = seanceService.trouverSeance(id);
        SeanceDetailVue detail = construireSeanceDetail(seance);
        model.addAttribute("seance", detail);
        return "reservation";
    }

    @PostMapping("/reserver")
    public String traiterReservation(
            @RequestParam("seanceId") Long seanceId,
            @RequestParam("emailClient") String emailClient,
            @RequestParam("nombrePlaces") Integer nombrePlaces,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        try {
            ReservationCreationRequest requete = new ReservationCreationRequest(seanceId, emailClient, nombrePlaces);
            ReservationResponse reponse = reservationService.creerReservation(requete);
            redirectAttributes.addFlashAttribute("messageSucces", "Réservation confirmée avec succès ! Votre billet est prêt.");
            return "redirect:/confirmation/" + reponse.code();
        } catch (CapaciteInsuffisanteException exception) {
            Seance seance = seanceService.trouverSeance(seanceId);
            int placesRestantes = reservationService.compterPlacesRestantes(seanceId, seance.getSalle().getCapacite());
            model.addAttribute("seance", construireSeanceDetail(seance));
            model.addAttribute("erreur", "Capacité insuffisante : il ne reste que " + placesRestantes + " place(s) disponible(s) pour cette séance.");
            model.addAttribute("emailPrecedente", emailClient);
            model.addAttribute("placesPrecedentes", nombrePlaces);
            return "reservation";
        } catch (Exception exception) {
            Seance seance = seanceService.trouverSeance(seanceId);
            model.addAttribute("seance", construireSeanceDetail(seance));
            model.addAttribute("erreur", "Erreur lors de la réservation : " + exception.getMessage());
            model.addAttribute("emailPrecedente", emailClient);
            model.addAttribute("placesPrecedentes", nombrePlaces);
            return "reservation";
        }
    }

    @GetMapping("/confirmation/{code}")
    public String afficherConfirmation(@PathVariable String code, Model model) {
        Reservation reservation = reservationService.trouverReservation(code);
        Seance seance = reservation.getSeance();
        Film film = seance.getFilm();

        BigDecimal montantTotal = seance.getPrixUnitaire().multiply(BigDecimal.valueOf(reservation.getNombrePlaces()));
        boolean estAnnulee = reservation.getStatut() == StatutReservation.ANNULEE;

        String dateFormatee = majusculeInitiale(seance.getDebut().format(FORMAT_DATE));
        String heureFormatee = seance.getDebut().format(FORMAT_HEURE);

        BilletConfirmationVue billet = new BilletConfirmationVue(
                reservation.getCode(),
                reservation.getEmailClient(),
                reservation.getNombrePlaces(),
                montantTotal,
                film.getTitre(),
                seance.getSalle().getNom(),
                seance.getDebut(),
                dateFormatee,
                heureFormatee,
                reservation.getStatut().name(),
                reservation.getCreeeLe(),
                estAnnulee
        );

        model.addAttribute("billet", billet);
        return "confirmation";
    }

    @PostMapping("/annuler/{code}")
    public String annulerReservation(
            @PathVariable String code,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reservationService.annulerReservation(code);
            redirectAttributes.addFlashAttribute("messageSucces", "La réservation " + code + " a été annulée avec succès. Les places ont été libérées.");
            return "redirect:/";
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("messageErreur", "Impossible d'annuler la réservation : " + exception.getMessage());
            return "redirect:/confirmation/" + code;
        }
    }

    @GetMapping("/admin/tableau-de-bord")
    public String afficherTableauDeBord(Model model) {
        List<Film> films = filmService.listerTousLesFilms();
        List<Seance> seances = seanceService.listerToutesLesSeances();

        List<SeanceResumeVue> vuesSeances = seances.stream()
                .map(this::construireSeanceResume)
                .toList();

        Counter compteurReservees = meterRegistry.find("billetterie.places.reservees").counter();
        long totalReservees = compteurReservees != null ? (long) compteurReservees.count() : 0;

        Counter compteurRefusees = meterRegistry.find("billetterie.places.refusees").counter();
        long totalRefusees = compteurRefusees != null ? (long) compteurRefusees.count() : 0;

        TableauDeBordVue tableau = new TableauDeBordVue(
                totalReservees,
                totalRefusees,
                films.size(),
                seances.size(),
                vuesSeances
        );

        model.addAttribute("tableau", tableau);
        return "tableau-de-bord";
    }

    private SeanceResumeVue construireSeanceResume(Seance seance) {
        int capacite = seance.getSalle().getCapacite();
        int reservees = reservationService.compterPlacesReservees(seance.getId());
        int restantes = Math.max(0, capacite - reservees);
        int pourcentage = capacite > 0 ? (int) Math.round(((double) reservees / capacite) * 100) : 0;

        String dateFormatee = majusculeInitiale(seance.getDebut().format(FORMAT_DATE));
        String heureFormatee = seance.getDebut().format(FORMAT_HEURE);

        return new SeanceResumeVue(
                seance.getId(),
                seance.getFilm().getId(),
                seance.getFilm().getTitre(),
                seance.getDebut(),
                dateFormatee,
                heureFormatee,
                seance.getPrixUnitaire(),
                seance.getSalle().getNom(),
                capacite,
                reservees,
                restantes,
                pourcentage,
                restantes == 0
        );
    }

    private SeanceDetailVue construireSeanceDetail(Seance seance) {
        Film film = seance.getFilm();
        int capacite = seance.getSalle().getCapacite();
        int reservees = reservationService.compterPlacesReservees(seance.getId());
        int restantes = Math.max(0, capacite - reservees);
        int pourcentage = capacite > 0 ? (int) Math.round(((double) reservees / capacite) * 100) : 0;

        String dateFormatee = majusculeInitiale(seance.getDebut().format(FORMAT_DATE));
        String heureFormatee = seance.getDebut().format(FORMAT_HEURE);
        String dureeFormatee = formaterDuree(film.getDureeMinutes());

        return new SeanceDetailVue(
                seance.getId(),
                film.getId(),
                film.getTitre(),
                film.getGenre(),
                film.getDureeMinutes(),
                dureeFormatee,
                seance.getSalle().getNom(),
                capacite,
                seance.getDebut(),
                dateFormatee,
                heureFormatee,
                seance.getPrixUnitaire(),
                reservees,
                restantes,
                pourcentage,
                restantes == 0
        );
    }

    private String formaterDuree(Integer minutes) {
        if (minutes == null || minutes <= 0) {
            return "-";
        }
        int heures = minutes / 60;
        int resteMinutes = minutes % 60;
        if (heures > 0 && resteMinutes > 0) {
            return heures + "h " + resteMinutes + "min";
        } else if (heures > 0) {
            return heures + "h";
        } else {
            return resteMinutes + "min";
        }
    }

    private String majusculeInitiale(String texte) {
        if (texte == null || texte.isEmpty()) {
            return texte;
        }
        return texte.substring(0, 1).toUpperCase(Locale.FRENCH) + texte.substring(1);
    }
}

