package com.alliance.quizmanagement.repository;

import com.alliance.quizmanagement.exception.StorageException;
import com.alliance.quizmanagement.model.Quiz;
import com.alliance.quizmanagement.service.FileStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;

/**
 * Repository for managing examinations (quizzes).
 */
public class QuizRepository {
    private static final String FILENAME = "quizzes.json";
    private final FileStorageService storageService;
    private final Map<String, Quiz> quizMap = new LinkedHashMap<>();

    public QuizRepository(FileStorageService storageService) throws StorageException {
        this.storageService = storageService;
        reload();
    }

    public synchronized void reload() throws StorageException {
        quizMap.clear();
        List<Quiz> list = storageService.loadList(FILENAME, new TypeReference<List<Quiz>>() {});
        for (Quiz q : list) {
            if (q.getId() != null) {
                quizMap.put(q.getId(), q);
            }
        }
    }

    public synchronized List<Quiz> findAll() {
        return new ArrayList<>(quizMap.values());
    }

    public synchronized Optional<Quiz> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(quizMap.get(id));
    }

    public synchronized void save(Quiz quiz) throws StorageException {
        if (quiz.getId() == null || quiz.getId().trim().isEmpty()) {
            quiz.setId("QUIZ-" + (System.currentTimeMillis() % 100000));
        }
        quizMap.put(quiz.getId(), quiz);
        storageService.saveList(FILENAME, new ArrayList<>(quizMap.values()));
    }

    public synchronized boolean deleteById(String id) throws StorageException {
        if (id != null && quizMap.containsKey(id)) {
            quizMap.remove(id);
            storageService.saveList(FILENAME, new ArrayList<>(quizMap.values()));
            return true;
        }
        return false;
    }
}
