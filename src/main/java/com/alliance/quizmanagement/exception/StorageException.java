package com.alliance.quizmanagement.exception;

/**
 * Thrown when file persistence operations encounter IO errors.
 */
public class StorageException extends QuizException {
    private static final long serialVersionUID = 1L;

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
