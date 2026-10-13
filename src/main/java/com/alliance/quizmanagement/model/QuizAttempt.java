package com.alliance.quizmanagement.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a complete student examination attempt.
 * Preserves statistical scores as well as granular question-level revision snapshots.
 */
public class QuizAttempt implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String quizId;
    private String quizTitle;
    private String studentUsername;
    private String studentFullName;

    private int totalQuestions;
    private int attemptedCount;
    private int unattemptedCount;
    private int correctCount;
    private int wrongCount;
    private int invalidCount;
    private int score;
    private int maxScore;
    private double percentage;

    private String startTime;
    private String submissionTime;
    private long timeTakenSeconds;
    private String submissionType; // "MANUAL" or "AUTOMATIC_TIMEOUT"

    private List<QuestionSnapshot> snapshots = new ArrayList<>();

    public QuizAttempt() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public String getQuizTitle() { return quizTitle; }
    public void setQuizTitle(String quizTitle) { this.quizTitle = quizTitle; }

    public String getStudentUsername() { return studentUsername; }
    public void setStudentUsername(String studentUsername) { this.studentUsername = studentUsername; }

    public String getStudentFullName() { return studentFullName; }
    public void setStudentFullName(String studentFullName) { this.studentFullName = studentFullName; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getAttemptedCount() { return attemptedCount; }
    public void setAttemptedCount(int attemptedCount) { this.attemptedCount = attemptedCount; }

    public int getUnattemptedCount() { return unattemptedCount; }
    public void setUnattemptedCount(int unattemptedCount) { this.unattemptedCount = unattemptedCount; }

    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }

    public int getWrongCount() { return wrongCount; }
    public void setWrongCount(int wrongCount) { this.wrongCount = wrongCount; }

    public int getInvalidCount() { return invalidCount; }
    public void setInvalidCount(int invalidCount) { this.invalidCount = invalidCount; }

    public int getUnansweredCount() { return unattemptedCount; }
    public void setUnansweredCount(int count) { this.unattemptedCount = count; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getSubmissionTime() { return submissionTime; }
    public void setSubmissionTime(String submissionTime) { this.submissionTime = submissionTime; }

    public long getTimeTakenSeconds() { return timeTakenSeconds; }
    public void setTimeTakenSeconds(long timeTakenSeconds) { this.timeTakenSeconds = timeTakenSeconds; }

    public String getSubmissionType() { return submissionType; }
    public void setSubmissionType(String submissionType) { this.submissionType = submissionType; }

    public List<QuestionSnapshot> getSnapshots() { return snapshots; }
    public void setSnapshots(List<QuestionSnapshot> snapshots) { this.snapshots = snapshots; }
}
