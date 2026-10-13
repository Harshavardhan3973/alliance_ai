package com.alliance.quizmanagement.service;

import com.alliance.quizmanagement.exception.StorageException;
import com.alliance.quizmanagement.exception.ValidationException;
import com.alliance.quizmanagement.model.Question;
import com.alliance.quizmanagement.model.Quiz;
import com.alliance.quizmanagement.repository.QuestionRepository;
import com.alliance.quizmanagement.repository.QuizRepository;
import java.util.*;

/**
 * Service managing Quiz configurations and question associations.
 */
public class QuizService {
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;

    public QuizService(QuizRepository quizRepository, QuestionRepository questionRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
    }

    public List<Quiz> getAllActiveQuizzes() {
        List<Quiz> list = new ArrayList<>();
        for (Quiz q : quizRepository.findAll()) {
            if (q.isActive()) {
                list.add(q);
            }
        }
        return list;
    }

    public Optional<Quiz> getQuizById(String id) {
        return quizRepository.findById(id);
    }

    public List<Question> getQuestionsForQuiz(Quiz quiz) throws ValidationException {
        if (quiz == null || quiz.getQuestionIds() == null || quiz.getQuestionIds().isEmpty()) {
            throw new ValidationException("Selected quiz has no questions associated with it.");
        }
        List<Question> questions = questionRepository.findByIds(quiz.getQuestionIds());
        if (questions.isEmpty()) {
            throw new ValidationException("None of the questions in the quiz could be found in the question bank.");
        }
        return questions;
    }

    public void createQuiz(Quiz quiz) throws ValidationException, StorageException {
        if (quiz == null) throw new ValidationException("Quiz cannot be null.");
        if (quiz.getTitle() == null || quiz.getTitle().trim().isEmpty()) {
            throw new ValidationException("Quiz title is required.");
        }
        if (quiz.getDurationMinutes() <= 0 || quiz.getDurationMinutes() > 180) {
            throw new ValidationException("Quiz duration must be between 1 and 180 minutes.");
        }
        if (quiz.getQuestionIds() == null || quiz.getQuestionIds().isEmpty()) {
            throw new ValidationException("A quiz must include at least one question.");
        }
        quizRepository.save(quiz);
    }

    public boolean deleteQuiz(String id) throws StorageException {
        return quizRepository.deleteById(id);
    }
}
