package com.alliance.quizmanagement.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Question entity representing a multiple-choice question in the question bank.
 * Linked directly to a specific Java concept and Course Outcome (CO-1, CO-2, CO-3).
 */
public class Question implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String questionText;
    private List<String> options = new ArrayList<>();
    private int correctAnswerIndex;
    private String concept;
    private Difficulty difficulty = Difficulty.MEDIUM;
    private String courseOutcome;
    private String explanation;

    public Question() {
    }

    public Question(String id, String questionText, List<String> options, int correctAnswerIndex,
                    String concept, Difficulty difficulty, String courseOutcome, String explanation) {
        this.id = id;
        this.questionText = questionText;
        this.options = options != null ? new ArrayList<>(options) : new ArrayList<>();
        this.correctAnswerIndex = correctAnswerIndex;
        this.concept = concept;
        this.difficulty = difficulty != null ? difficulty : Difficulty.MEDIUM;
        this.courseOutcome = courseOutcome;
        this.explanation = explanation;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public int getCorrectAnswerIndex() { return correctAnswerIndex; }
    public void setCorrectAnswerIndex(int correctAnswerIndex) { this.correctAnswerIndex = correctAnswerIndex; }

    public String getConcept() { return concept; }
    public void setConcept(String concept) { this.concept = concept; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public String getCourseOutcome() { return courseOutcome; }
    public void setCourseOutcome(String courseOutcome) { this.courseOutcome = courseOutcome; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getCorrectAnswerText() {
        if (options != null && correctAnswerIndex >= 0 && correctAnswerIndex < options.size()) {
            return options.get(correctAnswerIndex);
        }
        return "Unknown";
    }
}
