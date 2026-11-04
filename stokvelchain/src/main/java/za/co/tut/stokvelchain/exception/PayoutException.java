package za.co.tut.stokvelchain.exception;

public class PayoutException extends RuntimeException {
    public PayoutException(String message) {
        super(message);
    }
}