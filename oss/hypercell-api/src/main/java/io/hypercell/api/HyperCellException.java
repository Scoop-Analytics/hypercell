package io.hypercell.api;

public class HyperCellException extends RuntimeException {
    public HyperCellException(String message) {
        super(message);
    }

    public HyperCellException(String message, Throwable cause) {
        super(message, cause);
    }
}
