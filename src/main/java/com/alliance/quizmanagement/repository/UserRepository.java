package com.alliance.quizmanagement.repository;

import com.alliance.quizmanagement.exception.StorageException;
import com.alliance.quizmanagement.model.User;
import com.alliance.quizmanagement.service.FileStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;

/**
 * Repository interface & implementation for User accounts.
 */
public class UserRepository {
    private static final String FILENAME = "users.json";
    private final FileStorageService storageService;
    private final Map<String, User> userCache = new HashMap<>();

    public UserRepository(FileStorageService storageService) throws StorageException {
        this.storageService = storageService;
        reload();
    }

    public synchronized void reload() throws StorageException {
        userCache.clear();
        List<User> list = storageService.loadList(FILENAME, new TypeReference<List<User>>() {});
        for (User u : list) {
            if (u.getUsername() != null) {
                userCache.put(u.getUsername().toLowerCase(), u);
            }
        }
    }

    public synchronized Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(userCache.get(username.toLowerCase()));
    }

    public synchronized List<User> findAll() {
        return new ArrayList<>(userCache.values());
    }

    public synchronized void save(User user) throws StorageException {
        userCache.put(user.getUsername().toLowerCase(), user);
        storageService.saveList(FILENAME, new ArrayList<>(userCache.values()));
    }

    public synchronized boolean existsByUsername(String username) {
        if (username == null) return false;
        return userCache.containsKey(username.toLowerCase());
    }

    public synchronized boolean hasInstructorAccount() {
        return userCache.values().stream().anyMatch(u -> u.getRole() == com.alliance.quizmanagement.model.Role.INSTRUCTOR);
    }
}
