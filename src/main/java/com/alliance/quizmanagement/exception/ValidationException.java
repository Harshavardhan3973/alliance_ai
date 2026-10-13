package com.alliance.quizmanagement.exception;

/**
 * Thrown when business rules or input validation fails.
 */
public class ValidationException extends QuizException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
