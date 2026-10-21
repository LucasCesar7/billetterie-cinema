package fr.univ.billetterie.exception;

public class ConflitMetierException extends RuntimeException {

    private final String code;

    public ConflitMetierException(String message) {
        super(message);
        this.code = "CONFLIT_METIER";
    }

    public ConflitMetierException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

