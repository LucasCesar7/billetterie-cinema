package fr.univ.billetterie.exception;

public class CapaciteInsuffisanteException extends RuntimeException {

    private final String code;

    public CapaciteInsuffisanteException(String message) {
        super(message);
        this.code = "CAPACITE_INSUFFISANTE";
    }

    public CapaciteInsuffisanteException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

