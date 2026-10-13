package com.alliance.quizmanagement.service;

import com.alliance.quizmanagement.exception.StorageException;
import com.alliance.quizmanagement.exception.ValidationException;
import com.alliance.quizmanagement.model.Question;
import com.alliance.quizmanagement.model.QuestionSnapshot;
import com.alliance.quizmanagement.model.Quiz;
import com.alliance.quizmanagement.model.QuizAttempt;
import com.alliance.quizmanagement.model.User;
import com.alliance.quizmanagement.repository.AttemptRepository;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Authoritative business service for scoring examinations and building immutable revision snapshots.
 *
 * Enforces unified answer evaluation:
 * - Verified correct answer: marked CORRECT, awarded 1 mark
 * - Wrong answer: marked INCORRECT, awarded 0 marks
 * - Unanswered question: marked UNANSWERED, awarded 0 marks
 * - Invalid or missing option: marked INVALID (not correct), awarded 0 marks
 * - Invalid selected values are prevented from being saved as valid answers.
 */
public class ResultService {
    private final AttemptRepository attemptRepository;

    public ResultService(AttemptRepository attemptRepository) {
        this.attemptRepository = attemptRepository;
    }

    public QuizAttempt evaluateAndSaveAttempt(
            Quiz quiz,
            User student,
            List<Question> questions,
            Map<Integer, ?> answersByIndex, // questionIndex -> selectedOptionIndex or Option ID
            String startTimeStr,
            long timeTakenSeconds,
            String submissionType
    ) throws ValidationException, StorageException {

        if (quiz == null || student == null || questions == null || questions.isEmpty()) {
            throw new ValidationException("Cannot evaluate quiz: missing quiz, student, or question data.");
        }

        int total = questions.size();
        int attempted = 0;
        int correct = 0;
        int wrong = 0;
        int unanswered = 0;
        int invalid = 0;
        List<QuestionSnapshot> snapshots = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            Question q = questions.get(i);
            Object rawAnswer = answersByIndex != null ? answersByIndex.get(i) : null;
            int resolvedIndex = QuestionSnapshot.resolveOptionIndex(q, rawAnswer);

            QuestionSnapshot snapshot = new QuestionSnapshot(q, resolvedIndex);
            snapshots.add(snapshot);

            if (snapshot.isCorrect()) {
                correct++;
                attempted++;
            } else if (snapshot.isIncorrect()) {
                wrong++;
                attempted++;
            } else if (snapshot.isUnanswered()) {
                unanswered++;
            } else if (snapshot.isInvalid()) {
                invalid++;
            }
        }

        // Award 1 mark ONLY for verified correct answer; 0 for wrong, unanswered, or invalid answers
        int score = correct;
        int maxScore = total;
        double percentage = total > 0 ? Math.round(((double) score / (double) maxScore) * 10000.0) / 100.0 : 0.0;

        QuizAttempt attempt = new QuizAttempt();
        attempt.setId("ATT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        attempt.setQuizId(quiz.getId());
        attempt.setQuizTitle(quiz.getTitle());
        attempt.setStudentUsername(student.getUsername());
        attempt.setStudentFullName(student.getFullName());
        attempt.setTotalQuestions(total);
        attempt.setAttemptedCount(attempted);
        attempt.setUnattemptedCount(unanswered);
        attempt.setUnansweredCount(unanswered);
        attempt.setInvalidCount(invalid);
        attempt.setCorrectCount(correct);
        attempt.setWrongCount(wrong);
        attempt.setScore(score);
        attempt.setMaxScore(maxScore);
        attempt.setPercentage(percentage);
        attempt.setStartTime(startTimeStr != null ? startTimeStr : LocalDateTime.now().minusSeconds(timeTakenSeconds).toString());
        attempt.setSubmissionTime(LocalDateTime.now().toString());
        attempt.setTimeTakenSeconds(timeTakenSeconds);
        attempt.setSubmissionType(submissionType != null ? submissionType : "MANUAL");
        attempt.setSnapshots(snapshots);

        attemptRepository.save(attempt);
        return attempt;
    }

    /**
     * Overloaded submission helper allowing answers mapped by Question ID or option ID.
     */
    public QuizAttempt evaluateAndSaveAttemptByQuestionId(
            Quiz quiz,
            User student,
            List<Question> questions,
            Map<String, ?> answersByQuestionId,
            String startTimeStr,
            long timeTakenSeconds,
            String submissionType
    ) throws ValidationException, StorageException {
        Map<Integer, Object> answersByIndex = new HashMap<>();
        if (answersByQuestionId != null && questions != null) {
            for (int i = 0; i < questions.size(); i++) {
                Question q = questions.get(i);
                if (answersByQuestionId.containsKey(q.getId())) {
                    answersByIndex.put(i, answersByQuestionId.get(q.getId()));
                } else if (answersByQuestionId.containsKey(String.valueOf(i))) {
                    answersByIndex.put(i, answersByQuestionId.get(String.valueOf(i)));
                }
            }
        }
        return evaluateAndSaveAttempt(quiz, student, questions, answersByIndex, startTimeStr, timeTakenSeconds, submissionType);
    }

    public List<QuizAttempt> getStudentAttempts(String studentUsername) {
        return attemptRepository.findByStudentUsername(studentUsername);
    }

    public Optional<QuizAttempt> getAttemptById(String id) {
        return attemptRepository.findById(id);
    }

    public List<QuizAttempt> getAllAttempts() {
        return attemptRepository.findAll();
    }
}
