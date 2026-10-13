package com.alliance.quizmanagement.repository;

import com.alliance.quizmanagement.exception.StorageException;
import com.alliance.quizmanagement.model.Question;
import com.alliance.quizmanagement.service.FileStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;

/**
 * Repository for managing questions in the question bank.
 */
public class QuestionRepository {
    private static final String FILENAME = "questions.json";
    private final FileStorageService storageService;
    private final Map<String, Question> questionMap = new LinkedHashMap<>();

    public QuestionRepository(FileStorageService storageService) throws StorageException {
        this.storageService = storageService;
        reload();
    }

    public synchronized void reload() throws StorageException {
        questionMap.clear();
        List<Question> list = storageService.loadList(FILENAME, new TypeReference<List<Question>>() {});
        for (Question q : list) {
            if (q.getId() != null) {
                questionMap.put(q.getId(), q);
            }
        }
    }

    public synchronized List<Question> findAll() {
        return new ArrayList<>(questionMap.values());
    }

    public synchronized Optional<Question> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(questionMap.get(id));
    }

    public synchronized List<Question> findByIds(List<String> ids) {
        List<Question> result = new ArrayList<>();
        if (ids == null) return result;
        for (String id : ids) {
            Question q = questionMap.get(id);
            if (q != null) {
                result.add(q);
            }
        }
        return result;
    }

    public synchronized void save(Question question) throws StorageException {
        if (question.getId() == null || question.getId().trim().isEmpty()) {
            question.setId("Q-" + (System.currentTimeMillis() % 100000));
        }
        questionMap.put(question.getId(), question);
        storageService.saveList(FILENAME, new ArrayList<>(questionMap.values()));
    }

    public synchronized boolean deleteById(String id) throws StorageException {
        if (id != null && questionMap.containsKey(id)) {
            questionMap.remove(id);
            storageService.saveList(FILENAME, new ArrayList<>(questionMap.values()));
            return true;
        }
        return false;
    }
}
