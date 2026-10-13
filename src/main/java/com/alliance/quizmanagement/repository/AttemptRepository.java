package com.alliance.quizmanagement.repository;

import com.alliance.quizmanagement.exception.StorageException;
import com.alliance.quizmanagement.model.QuizAttempt;
import com.alliance.quizmanagement.service.FileStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;

/**
 * Repository for managing historical exam attempts and immutable question snapshots.
 */
public class AttemptRepository {
    private static final String FILENAME = "attempts.json";
    private final FileStorageService storageService;
    private final List<QuizAttempt> attemptList = new ArrayList<>();

    public AttemptRepository(FileStorageService storageService) throws StorageException {
        this.storageService = storageService;
        reload();
    }

    public synchronized void reload() throws StorageException {
        attemptList.clear();
        List<QuizAttempt> list = storageService.loadList(FILENAME, new TypeReference<List<QuizAttempt>>() {});
        attemptList.addAll(list);
    }

    public synchronized List<QuizAttempt> findAll() {
        return new ArrayList<>(attemptList);
    }

    public synchronized List<QuizAttempt> findByStudentUsername(String username) {
        List<QuizAttempt> result = new ArrayList<>();
        if (username == null) return result;
        for (QuizAttempt a : attemptList) {
            if (username.equalsIgnoreCase(a.getStudentUsername())) {
                result.add(a);
            }
        }
        return result;
    }

    public synchronized Optional<QuizAttempt> findById(String id) {
        if (id == null) return Optional.empty();
        return attemptList.stream().filter(a -> id.equalsIgnoreCase(a.getId())).findFirst();
    }

    public synchronized void save(QuizAttempt attempt) throws StorageException {
        if (attempt.getId() == null || attempt.getId().trim().isEmpty()) {
            attempt.setId("ATT-" + (System.currentTimeMillis() % 1000000));
        }
        // Remove existing if updating
        attemptList.removeIf(a -> a.getId().equalsIgnoreCase(attempt.getId()));
        attemptList.add(attempt);
        storageService.saveList(FILENAME, attemptList);
    }
}
