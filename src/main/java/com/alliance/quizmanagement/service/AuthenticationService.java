package com.alliance.quizmanagement.service;

import com.alliance.quizmanagement.exception.AuthenticationException;
import com.alliance.quizmanagement.exception.StorageException;
import com.alliance.quizmanagement.exception.ValidationException;
import com.alliance.quizmanagement.model.Instructor;
import com.alliance.quizmanagement.model.Role;
import com.alliance.quizmanagement.model.Student;
import com.alliance.quizmanagement.model.User;
import com.alliance.quizmanagement.repository.UserRepository;
import com.alliance.quizmanagement.security.PasswordUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service managing user authentication, registration, session state, and access control.
 */
public class AuthenticationService {
    private final UserRepository userRepository;
    private User currentUser;
    private final Map<String, Integer> failedAttemptsMap = new HashMap<>();

    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public synchronized User login(String username, String password) throws AuthenticationException {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Username and password are required.");
        }
        String cleanUser = username.trim().toLowerCase();

        int failed = failedAttemptsMap.getOrDefault(cleanUser, 0);
        if (failed >= 5) {
            throw new AuthenticationException("Account is temporarily locked due to too many failed attempts. Please try again later.");
        }

        Optional<User> opt = userRepository.findByUsername(cleanUser);
        if (opt.isEmpty()) {
            failedAttemptsMap.put(cleanUser, failed + 1);
            throw new AuthenticationException("Invalid credentials. Please verify your username and password.");
        }

        User user = opt.get();
        if (!user.isActive()) {
            throw new AuthenticationException("Account is inactive. Please contact your instructor.");
        }

        boolean valid = PasswordUtil.verifyPassword(password, user.getPasswordHash(), user.getPasswordSalt());
        if (!valid) {
            failedAttemptsMap.put(cleanUser, failed + 1);
            throw new AuthenticationException("Invalid credentials. Please verify your username and password.");
        }

        // Reset failed counter
        failedAttemptsMap.remove(cleanUser);
        this.currentUser = user;
        return user;
    }

    public synchronized Student registerStudent(String username, String fullName, String email,
                                                String password, String confirmPassword, String department)
            throws ValidationException, StorageException {
        if (username == null || username.trim().length() < 3) {
            throw new ValidationException("Username must be at least 3 characters long.");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Full name cannot be empty.");
        }
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long and include letters and numbers.");
        }
        if (!password.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match.");
        }
        if (!PasswordUtil.isValidPassword(password)) {
            throw new ValidationException("Password must contain at least one letter and one number.");
        }

        String cleanUser = username.trim().toLowerCase();
        if (userRepository.existsByUsername(cleanUser)) {
            throw new ValidationException("Username '" + cleanUser + "' is already registered.");
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);
        String id = "STU-" + (System.currentTimeMillis() % 100000);

        Student student = new Student(id, cleanUser, fullName.trim(), email != null ? email.trim() : "",
                salt, hash, department != null ? department.trim() : "School of Advanced Computing");

        userRepository.save(student);
        return student;
    }

    public synchronized Instructor setupFirstInstructor(String username, String fullName, String email,
                                                         String password, String designation, String department)
            throws ValidationException, StorageException {
        if (userRepository.hasInstructorAccount()) {
            throw new ValidationException("An instructor account is already configured in the system.");
        }
        if (username == null || username.trim().length() < 3) {
            throw new ValidationException("Username must be at least 3 characters long.");
        }
        if (password == null || password.length() < 6) {
            throw new ValidationException("Password must be at least 6 characters long.");
        }

        String cleanUser = username.trim().toLowerCase();
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);
        String id = "INS-001";

        Instructor instructor = new Instructor(id, cleanUser, fullName.trim(), email != null ? email.trim() : "",
                salt, hash, department, designation);

        userRepository.save(instructor);
        return instructor;
    }

    public synchronized void logout() {
        this.currentUser = null;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public synchronized void requireRole(Role requiredRole) throws AuthenticationException {
        if (currentUser == null) {
            throw new AuthenticationException("Session expired. Please log in.");
        }
        if (currentUser.getRole() != requiredRole) {
            throw new AuthenticationException("Access denied: You do not possess the required " + requiredRole + " privileges.");
        }
    }
}
