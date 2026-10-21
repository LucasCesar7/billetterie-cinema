package fr.univ.billetterie.exception;

public class RessourceIntrouvableException extends RuntimeException {

    private final String code;

    public RessourceIntrouvableException(String message) {
        super(message);
        this.code = "RESSOURCE_INTROUVABLE";
    }

    public RessourceIntrouvableException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

