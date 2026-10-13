package com.alliance.quizmanagement.exception;

/**
 * Thrown when credentials fail or unauthorized access is attempted.
 */
public class AuthenticationException extends QuizException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
