package com.alliance.quizmanagement.analytics;

import com.alliance.quizmanagement.model.QuestionSnapshot;
import com.alliance.quizmanagement.model.QuizAttempt;
import java.util.*;

/**
 * Evaluates and aggregates student performance across Java concepts.
 * Employs a HashMap for fast grouping and analysis (CO-2/CO-3: Java Collections Framework).
 */
public class ConceptAnalyticsService {

    /**
     * Computes concept analytics for a single quiz attempt.
     */
    public Map<String, ConceptPerformance> analyzeSingleAttempt(QuizAttempt attempt) {
        Map<String, ConceptPerformance> map = new HashMap<>();
        if (attempt == null || attempt.getSnapshots() == null) {
            return map;
        }

        for (QuestionSnapshot snapshot : attempt.getSnapshots()) {
            String concept = snapshot.getConcept();
            if (concept == null || concept.trim().isEmpty()) {
                concept = "General Java Core";
            }
            concept = concept.trim();

            ConceptPerformance performance = map.computeIfAbsent(concept, ConceptPerformance::new);
            performance.recordQuestion(snapshot);
        }

        return map;
    }

    /**
     * Computes aggregate concept analytics across all past attempts for a given student.
     */
    public Map<String, ConceptPerformance> analyzeAggregateStudentAttempts(List<QuizAttempt> attempts, String studentUsername) {
        Map<String, ConceptPerformance> map = new HashMap<>();
        if (attempts == null) {
            return map;
        }

        for (QuizAttempt attempt : attempts) {
            if (studentUsername != null && !studentUsername.equalsIgnoreCase(attempt.getStudentUsername())) {
                continue;
            }
            if (attempt.getSnapshots() == null) continue;

            for (QuestionSnapshot snapshot : attempt.getSnapshots()) {
                String concept = snapshot.getConcept();
                if (concept == null || concept.trim().isEmpty()) {
                    concept = "General Java Core";
                }
                concept = concept.trim();

                ConceptPerformance performance = map.computeIfAbsent(concept, ConceptPerformance::new);
                performance.recordQuestion(snapshot);
            }
        }

        return map;
    }

    /**
     * Returns concepts filtered by weak status (&lt; 60% accuracy).
     */
    public List<ConceptPerformance> getWeakConcepts(Map<String, ConceptPerformance> performanceMap) {
        List<ConceptPerformance> weakList = new ArrayList<>();
        for (ConceptPerformance perf : performanceMap.values()) {
            if ("WEAK".equalsIgnoreCase(perf.getStatusLevel())) {
                weakList.add(perf);
            }
        }
        weakList.sort(Comparator.comparingDouble(ConceptPerformance::getAccuracyPercentage));
        return weakList;
    }

    /**
     * Returns all historical snapshots for a particular concept so students can revise them.
     */
    public List<QuestionSnapshot> findQuestionsByConcept(List<QuizAttempt> attempts, String studentUsername, String concept) {
        List<QuestionSnapshot> results = new ArrayList<>();
        if (attempts == null || concept == null) return results;

        for (QuizAttempt attempt : attempts) {
            if (studentUsername != null && !studentUsername.equalsIgnoreCase(attempt.getStudentUsername())) {
                continue;
            }
            if (attempt.getSnapshots() == null) continue;

            for (QuestionSnapshot snapshot : attempt.getSnapshots()) {
                if (concept.equalsIgnoreCase(snapshot.getConcept())) {
                    results.add(snapshot);
                }
            }
        }
        return results;
    }
}
