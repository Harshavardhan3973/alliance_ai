package com.alliance.quizmanagement.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Quiz entity representing a configured examination.
 */
public class Quiz implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String description;
    private int durationMinutes = 10;
    private int passPercentage = 60;
    private String createdBy;
    private List<String> questionIds = new ArrayList<>();
    private String courseOutcome = "CO-1, CO-2, CO-3";
    private boolean active = true;
    private List<String> instructions = new ArrayList<>();

    public Quiz() {
    }

    public Quiz(String id, String title, String description, int durationMinutes,
                int passPercentage, String createdBy, List<String> questionIds,
                String courseOutcome, List<String> instructions) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.passPercentage = passPercentage;
        this.createdBy = createdBy;
        this.questionIds = questionIds != null ? new ArrayList<>(questionIds) : new ArrayList<>();
        this.courseOutcome = courseOutcome;
        this.instructions = instructions != null ? new ArrayList<>(instructions) : new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getPassPercentage() { return passPercentage; }
    public void setPassPercentage(int passPercentage) { this.passPercentage = passPercentage; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public List<String> getQuestionIds() { return questionIds; }
    public void setQuestionIds(List<String> questionIds) { this.questionIds = questionIds; }

    public String getCourseOutcome() { return courseOutcome; }
    public void setCourseOutcome(String courseOutcome) { this.courseOutcome = courseOutcome; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<String> getInstructions() { return instructions; }
    public void setInstructions(List<String> instructions) { this.instructions = instructions; }
}
