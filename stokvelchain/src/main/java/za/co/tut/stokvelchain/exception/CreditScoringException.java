package za.co.tut.stokvelchain.exception;

public class CreditScoringException extends RuntimeException {
    public CreditScoringException(String message) {
        super(message);
    }

    public CreditScoringException(String message, Throwable cause) {
        super(message, cause);
    }
}