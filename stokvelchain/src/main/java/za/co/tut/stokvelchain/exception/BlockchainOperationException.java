package za.co.tut.stokvelchain.exception;

public class BlockchainOperationException extends RuntimeException{
    public BlockchainOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
