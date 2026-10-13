package com.alliance.quizmanagement.analytics;

import com.alliance.quizmanagement.model.QuestionSnapshot;
import java.io.Serializable;

/**
 * Encapsulates performance metrics for a specific Java concept.
 * Tracks verified correct (+1), wrong (0), unanswered (0), and invalid (0) questions.
 */
public class ConceptPerformance implements Serializable {
    private static final long serialVersionUID = 1L;

    private String concept;
    private int totalQuestions;
    private int attempted;
    private int correct;
    private int wrong;
    private int unattempted;
    private int invalid;
    private double accuracyPercentage;
    private String recommendation; // "Needs revision", "Practice more", "Good performance", "Not attempted"
    private String statusLevel; // "WEAK", "MODERATE", "STRONG", "UNATTEMPTED"

    public ConceptPerformance(String concept) {
        this.concept = concept;
        this.totalQuestions = 0;
        this.attempted = 0;
        this.correct = 0;
        this.wrong = 0;
        this.unattempted = 0;
        this.invalid = 0;
        this.accuracyPercentage = 0.0;
        this.recommendation = "Not attempted";
        this.statusLevel = "UNATTEMPTED";
    }

    public void recordQuestion(QuestionSnapshot snapshot) {
        if (snapshot == null) return;
        this.totalQuestions++;
        if (snapshot.isCorrect()) {
            this.attempted++;
            this.correct++;
        } else if (snapshot.isIncorrect()) {
            this.attempted++;
            this.wrong++;
        } else if (snapshot.isUnanswered()) {
            this.unattempted++;
        } else if (snapshot.isInvalid()) {
            this.invalid++;
        }
        recalculate();
    }

    public void recordQuestion(boolean isAttempted, boolean isCorrect) {
        this.totalQuestions++;
        if (isAttempted) {
            this.attempted++;
            if (isCorrect) {
                this.correct++;
            } else {
                this.wrong++;
            }
        } else {
            this.unattempted++;
        }
        recalculate();
    }

    public void recalculate() {
        if (attempted == 0) {
            this.accuracyPercentage = 0.0;
            this.recommendation = "Not attempted";
            this.statusLevel = "UNATTEMPTED";
        } else {
            this.accuracyPercentage = Math.round(((double) correct / (double) attempted) * 10000.0) / 100.0;
            if (accuracyPercentage < 60.0) {
                this.recommendation = "Needs revision";
                this.statusLevel = "WEAK";
            } else if (accuracyPercentage < 80.0) {
                this.recommendation = "Practice more";
                this.statusLevel = "MODERATE";
            } else {
                this.recommendation = "Good performance";
                this.statusLevel = "STRONG";
            }
        }
    }

    public String getConcept() { return concept; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getAttempted() { return attempted; }
    public int getCorrect() { return correct; }
    public int getWrong() { return wrong; }
    public int getUnattempted() { return unattempted; }
    public int getUnanswered() { return unattempted; }
    public int getInvalid() { return invalid; }
    public void setInvalid(int invalid) { this.invalid = invalid; }
    public double getAccuracyPercentage() { return accuracyPercentage; }
    public String getRecommendation() { return recommendation; }
    public String getStatusLevel() { return statusLevel; }

    @Override
    public String toString() {
        return String.format("%s: %d/%d correct (%.2f%%) - %s",
                concept, correct, attempted, accuracyPercentage, recommendation);
    }
}
