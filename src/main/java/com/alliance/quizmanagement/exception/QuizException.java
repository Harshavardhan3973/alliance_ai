package com.alliance.quizmanagement.exception;

/**
 * Base custom application exception.
 * Demonstrates CO-3: Custom exception hierarchy.
 */
public class QuizException extends Exception {
    private static final long serialVersionUID = 1L;

    public QuizException(String message) {
        super(message);
    }

    public QuizException(String message, Throwable cause) {
        super(message, cause);
    }
}
