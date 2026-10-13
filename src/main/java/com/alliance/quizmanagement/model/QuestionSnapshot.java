package com.alliance.quizmanagement.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable snapshot of a question as presented during an examination attempt.
 * Preserves the exact text, options, student choice, correct answer, and Java concept
 * even if the question bank is subsequently edited or deleted.
 *
 * Implements strict four-case evaluation:
 * 1. CORRECT: Verified correct answer -> 1 mark
 * 2. INCORRECT: Wrong answer -> 0 marks
 * 3. UNANSWERED: Unanswered question -> 0 marks
 * 4. INVALID: Missing or invalid option/configuration -> 0 marks (never correct)
 */
public class QuestionSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_CORRECT = "CORRECT";
    public static final String STATUS_INCORRECT = "INCORRECT";
    public static final String STATUS_UNANSWERED = "UNANSWERED";
    public static final String STATUS_INVALID = "INVALID";

    private String questionId;
    private String questionText;
    private List<String> options = new ArrayList<>();
    private int correctAnswerIndex = -1;
    private int studentAnswerIndex = -1; // -1 indicates unattempted / unanswered / invalid
    private String concept;
    private String courseOutcome;
    private String explanation;
    private String status = STATUS_UNANSWERED;

    public QuestionSnapshot() {
    }

    public QuestionSnapshot(Question original, Object rawSelectedOption) {
        this(original, resolveOptionIndex(original, rawSelectedOption));
    }

    public QuestionSnapshot(Question original, int studentAnswerIndex) {
        if (original == null) {
            this.status = STATUS_INVALID;
            this.studentAnswerIndex = -1;
            return;
        }

        this.questionId = original.getId();
        this.questionText = original.getQuestionText();
        this.options = original.getOptions() != null ? new ArrayList<>(original.getOptions()) : new ArrayList<>();
        this.correctAnswerIndex = original.getCorrectAnswerIndex();
        this.concept = original.getConcept();
        this.courseOutcome = original.getCourseOutcome();
        this.explanation = original.getExplanation();

        evaluate(studentAnswerIndex);
    }

    /**
     * Authoritative four-case evaluation logic:
     * - A correct answer is marked CORRECT and awarded 1 mark.
     * - A wrong answer is marked INCORRECT and awarded 0 marks.
     * - An unanswered question is marked UNANSWERED and awarded 0 marks.
     * - An invalid or missing option is marked INVALID (not correct) and awarded 0 marks.
     * Invalid values are strictly normalized so they are never saved as valid options.
     */
    private void evaluate(int rawStudentIndex) {
        // Step 1: Validate question integrity (options exist and correct answer index is within bounds)
        boolean hasValidOptions = this.options != null && !this.options.isEmpty();
        boolean hasValidCorrectAnswer = hasValidOptions
                && this.correctAnswerIndex >= 0
                && this.correctAnswerIndex < this.options.size();

        // Step 2: Validate student selection
        if (rawStudentIndex == -1) {
            // Case 3: Unanswered question
            this.status = STATUS_UNANSWERED;
            this.studentAnswerIndex = -1;
        } else if (!hasValidOptions || !hasValidCorrectAnswer) {
            // Case 4: Missing or invalid question configuration -> INVALID (never correct)
            this.status = STATUS_INVALID;
            this.studentAnswerIndex = -1; // Prevent invalid values from being saved as valid answers
        } else if (rawStudentIndex < -1 || rawStudentIndex >= this.options.size()) {
            // Case 4: Selected option index out-of-bounds -> INVALID (never correct)
            this.status = STATUS_INVALID;
            this.studentAnswerIndex = -1; // Prevent invalid values from being saved as valid answers
        } else if (rawStudentIndex == this.correctAnswerIndex) {
            // Case 1: Verified correct answer
            this.status = STATUS_CORRECT;
            this.studentAnswerIndex = rawStudentIndex;
        } else {
            // Case 2: Wrong answer
            this.status = STATUS_INCORRECT;
            this.studentAnswerIndex = rawStudentIndex;
        }
    }

    /**
     * Resolves selected option input (Integer index, String ID, option text, letter)
     * against the question's actual options.
     * Returns:
     *  -1 for unanswered / null
     *  valid index [0..size-1] if matched
     *  -999 for invalid / non-existent option
     */
    public static int resolveOptionIndex(Question q, Object rawOption) {
        if (rawOption == null) {
            return -1;
        }
        if (rawOption instanceof Number) {
            int val = ((Number) rawOption).intValue();
            if (val == -1) return -1;
            if (q == null || q.getOptions() == null) return -999;
            if (val >= 0 && val < q.getOptions().size()) {
                return val;
            }
            return -999; // Out-of-bounds number
        }

        String str = rawOption.toString().trim();
        if (str.isEmpty() || "-1".equals(str) || "unanswered".equalsIgnoreCase(str) || "null".equalsIgnoreCase(str)) {
            return -1;
        }

        // Try direct integer string
        try {
            int parsed = Integer.parseInt(str);
            if (parsed == -1) return -1;
            if (q == null || q.getOptions() == null) return -999;
            if (parsed >= 0 && parsed < q.getOptions().size()) {
                return parsed;
            }
            return -999; // Numeric index is out of bounds
        } catch (NumberFormatException ignored) {}

        if (q == null || q.getOptions() == null || q.getOptions().isEmpty()) {
            return -999;
        }

        List<String> opts = q.getOptions();

        // Check single letter choice (A -> 0, B -> 1, C -> 2, D -> 3)
        if (str.length() == 1) {
            char ch = Character.toUpperCase(str.charAt(0));
            int letterIndex = ch - 'A';
            if (letterIndex >= 0 && letterIndex < opts.size()) {
                return letterIndex;
            }
        }

        // Check prefix like "opt-0", "opt_1", "option 2"
        String lower = str.toLowerCase();
        if (lower.startsWith("opt")) {
            String digits = lower.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                try {
                    int parsed = Integer.parseInt(digits);
                    if (parsed >= 0 && parsed < opts.size()) {
                        return parsed;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // Check exact match of option text
        for (int i = 0; i < opts.size(); i++) {
            if (opts.get(i).equalsIgnoreCase(str)) {
                return i;
            }
        }

        // Unrecognized / non-existent option
        return -999;
    }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public int getCorrectAnswerIndex() { return correctAnswerIndex; }
    public void setCorrectAnswerIndex(int correctAnswerIndex) { this.correctAnswerIndex = correctAnswerIndex; }

    public int getStudentAnswerIndex() { return studentAnswerIndex; }
    public void setStudentAnswerIndex(int studentAnswerIndex) { this.studentAnswerIndex = studentAnswerIndex; }

    public String getConcept() { return concept; }
    public void setConcept(String concept) { this.concept = concept; }

    public String getCourseOutcome() { return courseOutcome; }
    public void setCourseOutcome(String courseOutcome) { this.courseOutcome = courseOutcome; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isAttempted() {
        return STATUS_CORRECT.equalsIgnoreCase(status) || STATUS_INCORRECT.equalsIgnoreCase(status);
    }

    public boolean isCorrect() {
        return STATUS_CORRECT.equalsIgnoreCase(status);
    }

    public boolean isIncorrect() {
        return STATUS_INCORRECT.equalsIgnoreCase(status);
    }

    public boolean isUnanswered() {
        return STATUS_UNANSWERED.equalsIgnoreCase(status) || "UNATTEMPTED".equalsIgnoreCase(status);
    }

    public boolean isInvalid() {
        return STATUS_INVALID.equalsIgnoreCase(status);
    }

    /**
     * Award 1 mark ONLY for a verified correct answer; award 0 for wrong, unanswered, or invalid answers.
     */
    public int getMarksAwarded() {
        return isCorrect() ? 1 : 0;
    }

    public String getStudentAnswerText() {
        if (isInvalid()) {
            return "Invalid Option";
        }
        if (isUnanswered() || studentAnswerIndex < 0 || options == null || studentAnswerIndex >= options.size()) {
            return "Unanswered";
        }
        return options.get(studentAnswerIndex);
    }

    public String getCorrectAnswerText() {
        if (options != null && correctAnswerIndex >= 0 && correctAnswerIndex < options.size()) {
            return options.get(correctAnswerIndex);
        }
        return "Not configured / Invalid";
    }
}
