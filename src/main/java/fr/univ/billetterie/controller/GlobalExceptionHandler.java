package fr.univ.billetterie.controller;

import fr.univ.billetterie.dto.ErreurResponse;
import fr.univ.billetterie.exception.CapaciteInsuffisanteException;
import fr.univ.billetterie.exception.ConflitMetierException;
import fr.univ.billetterie.exception.RessourceIntrouvableException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurResponse> gererValidationEchouee(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String messageDetaille = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        ErreurResponse corpsErreur = new ErreurResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ECHOUEE",
                messageDetaille.isEmpty() ? "Les données fournies sont invalides" : messageDetaille,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(corpsErreur);
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ErreurResponse> gererRessourceIntrouvable(
            RessourceIntrouvableException exception,
            HttpServletRequest request
    ) {
        ErreurResponse corpsErreur = new ErreurResponse(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                exception.getCode(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(corpsErreur);
    }

    @ExceptionHandler(CapaciteInsuffisanteException.class)
    public ResponseEntity<ErreurResponse> gererCapaciteInsuffisante(
            CapaciteInsuffisanteException exception,
            HttpServletRequest request
    ) {
        ErreurResponse corpsErreur = new ErreurResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                exception.getCode(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(corpsErreur);
    }

    @ExceptionHandler(ConflitMetierException.class)
    public ResponseEntity<ErreurResponse> gererConflitMetier(
            ConflitMetierException exception,
            HttpServletRequest request
    ) {
        ErreurResponse corpsErreur = new ErreurResponse(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                exception.getCode(),
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(corpsErreur);
    }
}

