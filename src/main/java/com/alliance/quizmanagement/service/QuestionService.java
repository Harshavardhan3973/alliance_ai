package com.alliance.quizmanagement.service;

import com.alliance.quizmanagement.exception.StorageException;
import com.alliance.quizmanagement.exception.ValidationException;
import com.alliance.quizmanagement.model.Question;
import com.alliance.quizmanagement.repository.QuestionRepository;
import java.util.*;

/**
 * Service managing Question bank operations, validation, and filtering.
 */
public class QuestionService {
    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<Question> getAllQuestions() {
        return questionRepository.findAll();
    }

    public Optional<Question> getQuestionById(String id) {
        return questionRepository.findById(id);
    }

    public void saveOrUpdateQuestion(Question question) throws ValidationException, StorageException {
        validateQuestion(question);
        questionRepository.save(question);
    }

    public boolean deleteQuestion(String id) throws StorageException {
        return questionRepository.deleteById(id);
    }

    public List<String> getAllDistinctConcepts() {
        Set<String> concepts = new TreeSet<>();
        for (Question q : questionRepository.findAll()) {
            if (q.getConcept() != null && !q.getConcept().trim().isEmpty()) {
                concepts.add(q.getConcept().trim());
            }
        }
        return new ArrayList<>(concepts);
    }

    public List<Question> getQuestionsByConcept(String concept) {
        List<Question> list = new ArrayList<>();
        if (concept == null) return list;
        for (Question q : questionRepository.findAll()) {
            if (concept.equalsIgnoreCase(q.getConcept())) {
                list.add(q);
            }
        }
        return list;
    }

    private void validateQuestion(Question q) throws ValidationException {
        if (q == null) {
            throw new ValidationException("Question object cannot be null.");
        }
        if (q.getQuestionText() == null || q.getQuestionText().trim().isEmpty()) {
            throw new ValidationException("Question text is required.");
        }
        if (q.getOptions() == null || q.getOptions().size() < 2) {
            throw new ValidationException("Question must have at least 2 answer choices.");
        }
        for (String opt : q.getOptions()) {
            if (opt == null || opt.trim().isEmpty()) {
                throw new ValidationException("Answer option text cannot be blank.");
            }
        }
        if (q.getCorrectAnswerIndex() < 0 || q.getCorrectAnswerIndex() >= q.getOptions().size()) {
            throw new ValidationException("Correct answer index is out of bounds.");
        }
        if (q.getConcept() == null || q.getConcept().trim().isEmpty()) {
            throw new ValidationException("Every question must be linked to a valid Java concept.");
        }
    }
}
