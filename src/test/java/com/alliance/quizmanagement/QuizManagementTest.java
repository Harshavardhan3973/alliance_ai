package com.alliance.quizmanagement;

import com.alliance.quizmanagement.analytics.ConceptAnalyticsService;
import com.alliance.quizmanagement.analytics.ConceptPerformance;
import com.alliance.quizmanagement.exception.*;
import com.alliance.quizmanagement.model.*;
import com.alliance.quizmanagement.repository.*;
import com.alliance.quizmanagement.security.PasswordUtil;
import com.alliance.quizmanagement.service.*;
import com.alliance.quizmanagement.util.QuizTimer;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 Test Suite for Alliance University Online Quiz Management System.
 * Tests CO-1, CO-2, CO-3 compliance, security, scoring, snapshot immutability, and concept analytics.
 */
public class QuizManagementTest {

    private File tempDir;
    private FileStorageService storageService;
    private UserRepository userRepo;
    private QuestionRepository questionRepo;
    private QuizRepository quizRepo;
    private AttemptRepository attemptRepo;
    private AuthenticationService authService;
    private QuestionService questionService;
    private QuizService quizService;
    private ResultService resultService;
    private ConceptAnalyticsService analyticsService;

    @BeforeEach
    public void setup() throws IOException, StorageException {
        tempDir = Files.createTempDirectory("quiz_test_data").toFile();
        storageService = new FileStorageService(tempDir);
        userRepo = new UserRepository(storageService);
        questionRepo = new QuestionRepository(storageService);
        quizRepo = new QuizRepository(storageService);
        attemptRepo = new AttemptRepository(storageService);

        authService = new AuthenticationService(userRepo);
        questionService = new QuestionService(questionRepo);
        quizService = new QuizService(quizRepo, questionRepo);
        resultService = new ResultService(attemptRepo);
        analyticsService = new ConceptAnalyticsService();
    }

    @AfterEach
    public void tearDown() {
        if (tempDir != null && tempDir.exists()) {
            for (File f : tempDir.listFiles()) {
                f.delete();
            }
            tempDir.delete();
        }
    }

    @Test
    @DisplayName("CO-1: Test Object-Oriented Polymorphism & Inheritance for Users")
    public void testUserPolymorphism() {
        User student = new Student("STU-1", "monisha", "Monisha R", "monisha@alliance.edu", "salt", "hash", "CS");
        User instructor = new Instructor("INS-1", "rajesh", "Prof Rajesh", "rajesh@alliance.edu", "salt", "hash", "CS", "Asst Prof");

        assertEquals(Role.STUDENT, student.getRole());
        assertEquals("Student Candidate", student.getDisplayRoleTitle());

        assertEquals(Role.INSTRUCTOR, instructor.getRole());
        assertTrue(instructor.getDisplayRoleTitle().contains("Course Instructor"));
    }

    @Test
    @DisplayName("Security: PBKDF2 Password Hashing and Verification")
    public void testPasswordHashing() {
        String password = "SecurePassword123";
        String salt = PasswordUtil.generateSalt();
        assertNotNull(salt);

        String hash = PasswordUtil.hashPassword(password, salt);
        assertNotNull(hash);

        assertTrue(PasswordUtil.verifyPassword(password, hash, salt));
        assertFalse(PasswordUtil.verifyPassword("WrongPassword", hash, salt));
        assertFalse(PasswordUtil.verifyPassword(password, "invalidhash", salt));
    }

    @Test
    @DisplayName("CO-3: Exception Handling on Invalid Registration Input")
    public void testInvalidRegistrationThrowsException() {
        assertThrows(ValidationException.class, () -> {
            authService.registerStudent("ab", "Harsha", "harsha@alliance.edu", "Pass123", "Pass123", "CS");
        }, "Should reject username under 3 characters");

        assertThrows(ValidationException.class, () -> {
            authService.registerStudent("harsha_v", "Harsha", "harsha@alliance.edu", "Pass123", "Mismatch456", "CS");
        }, "Should reject mismatched passwords");
    }

    @Test
    @DisplayName("Authentication: Registration and Duplicate Prevention")
    public void testStudentRegistrationAndLogin() throws Exception {
        Student registered = authService.registerStudent(
                "dheeraj_k",
                "K Dheeraj Chowdary",
                "dheeraj@ced.alliance.edu.in",
                "Password123",
                "Password123",
                "School of Advanced Computing"
        );
        assertNotNull(registered);
        assertTrue(userRepo.existsByUsername("dheeraj_k"));

        // Duplicate registration must fail
        assertThrows(ValidationException.class, () -> {
            authService.registerStudent("dheeraj_k", "Duplicate", "email", "Pass123", "Pass123", "CS");
        });

        // Login with correct credentials
        User loggedIn = authService.login("dheeraj_k", "Password123");
        assertNotNull(loggedIn);
        assertEquals("dheeraj_k", loggedIn.getUsername());

        // Login with bad password
        assertThrows(AuthenticationException.class, () -> {
            authService.login("dheeraj_k", "IncorrectPass");
        });
    }

    @Test
    @DisplayName("Scoring: Authoritative Evaluation (1 mark correct, 0 for wrong/unattempted)")
    public void testQuizEvaluationAndScoring() throws Exception {
        Question q1 = new Question("Q1", "Java inheritance keyword?", List.of("implements", "extends", "inherits", "super"), 1, "Inheritance", Difficulty.EASY, "CO-1", "Use extends");
        Question q2 = new Question("Q2", "Java lock keyword?", List.of("lock", "atomic", "synchronized", "volatile"), 2, "Multithreading", Difficulty.MEDIUM, "CO-3", "Use synchronized");
        questionRepo.save(q1);
        questionRepo.save(q2);

        Quiz quiz = new Quiz("QUIZ-T", "Test Exam", "Desc", 5, 60, "prof", List.of("Q1", "Q2"), "CO-1, CO-3", List.of());
        quizRepo.save(quiz);

        User student = new Student("STU-1", "kadalinga", "Kadalinga", "", "s", "h", "CS");

        // Answers: Q1 correct (index 1), Q2 unattempted (-1)
        Map<Integer, Integer> answers = new HashMap<>();
        answers.put(0, 1); // correct
        // index 1 left unattempted

        QuizAttempt attempt = resultService.evaluateAndSaveAttempt(
                quiz,
                student,
                List.of(q1, q2),
                answers,
                "2026-10-13T10:00:00",
                60,
                "MANUAL"
        );

        assertEquals(2, attempt.getTotalQuestions());
        assertEquals(1, attempt.getAttemptedCount());
        assertEquals(1, attempt.getUnattemptedCount());
        assertEquals(1, attempt.getCorrectCount());
        assertEquals(0, attempt.getWrongCount());
        assertEquals(1, attempt.getScore());
        assertEquals(50.0, attempt.getPercentage());
    }

    @Test
    @DisplayName("Innovation: Immutable Question Snapshots Retain Historical Data")
    public void testImmutableSnapshotPreservation() throws Exception {
        Question originalQ = new Question("Q-SNAP", "Original Question Text", List.of("A", "B", "C", "D"), 0, "Polymorphism", Difficulty.EASY, "CO-2", "Exp");
        questionRepo.save(originalQ);

        Quiz quiz = new Quiz("QZ-1", "Snap Quiz", "", 5, 50, "p", List.of("Q-SNAP"), "CO-2", List.of());
        quizRepo.save(quiz);

        User student = new Student("S1", "thanushree", "Thanushree", "", "s", "h", "CS");
        Map<Integer, Integer> answers = Map.of(0, 0);

        QuizAttempt attempt = resultService.evaluateAndSaveAttempt(
                quiz, student, List.of(originalQ), answers, "now", 30, "MANUAL"
        );

        // Now maliciously edit or delete original question in question bank
        originalQ.setQuestionText("MODIFIED QUESTION TEXT POST-EXAM");
        originalQ.setCorrectAnswerIndex(2);
        questionRepo.save(originalQ);

        // Fetch saved attempt and verify snapshot was NOT mutated
        QuizAttempt savedAttempt = attemptRepo.findById(attempt.getId()).orElseThrow();
        QuestionSnapshot snapshot = savedAttempt.getSnapshots().get(0);

        assertEquals("Original Question Text", snapshot.getQuestionText(), "Historical attempt must remain immutable");
        assertEquals(0, snapshot.getCorrectAnswerIndex(), "Correct answer snapshot must remain unmodified");
    }

    @Test
    @DisplayName("Innovation: Concept-Wise HashMap Analytics Detects Weak Concepts (<60%)")
    public void testConceptWiseAnalyticsHashMap() {
        Question qInheritance1 = new Question("Q1", "T1", List.of("A", "B"), 0, "Inheritance", Difficulty.EASY, "CO-1", "");
        Question qInheritance2 = new Question("Q2", "T2", List.of("A", "B"), 0, "Inheritance", Difficulty.EASY, "CO-1", "");
        Question qPoly = new Question("Q3", "T3", List.of("A", "B"), 0, "Polymorphism", Difficulty.EASY, "CO-2", "");

        // Snapshot 1: Inheritance Wrong (0 correct out of 2)
        QuestionSnapshot s1 = new QuestionSnapshot(qInheritance1, 1); // incorrect
        QuestionSnapshot s2 = new QuestionSnapshot(qInheritance2, 1); // incorrect

        // Snapshot 2: Polymorphism Correct (1 correct out of 1)
        QuestionSnapshot s3 = new QuestionSnapshot(qPoly, 0); // correct

        QuizAttempt attempt = new QuizAttempt();
        attempt.setStudentUsername("harsha_v");
        attempt.setSnapshots(List.of(s1, s2, s3));

        Map<String, ConceptPerformance> map = analyticsService.analyzeSingleAttempt(attempt);

        assertNotNull(map.get("Inheritance"));
        assertNotNull(map.get("Polymorphism"));

        ConceptPerformance inhPerf = map.get("Inheritance");
        assertEquals(0.0, inhPerf.getAccuracyPercentage());
        assertEquals("Needs revision", inhPerf.getRecommendation());
        assertEquals("WEAK", inhPerf.getStatusLevel());

        ConceptPerformance polyPerf = map.get("Polymorphism");
        assertEquals(100.0, polyPerf.getAccuracyPercentage());
        assertEquals("Good performance", polyPerf.getRecommendation());
        assertEquals("STRONG", polyPerf.getStatusLevel());

        List<ConceptPerformance> weak = analyticsService.getWeakConcepts(map);
        assertEquals(1, weak.size());
        assertEquals("Inheritance", weak.get(0).getConcept());
    }

    @Test
    @DisplayName("CO-3: Multithreaded QuizTimer Concurrency & Expiration")
    public void testQuizTimerCountdown() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        final boolean[] timedOut = {false};

        // 1-second timer
        QuizTimer timer = new QuizTimer(1, new QuizTimer.TimerListener() {
            @Override
            public void onTick(long remainingSeconds, String formattedTime) {
            }

            @Override
            public void onTimeout() {
                timedOut[0] = true;
                latch.countDown();
            }
        });

        timer.start();
        boolean finishedInTime = latch.await(3, TimeUnit.SECONDS);

        assertTrue(finishedInTime, "Timer listener should have received timeout signal within 3s");
        assertTrue(timedOut[0], "Timeout flag should be true");
        assertTrue(timer.isFinished());
    }

    @Test
    @DisplayName("Instructor: Question Bank CRUD Operations")
    public void testInstructorQuestionManagement() throws Exception {
        // 1. Create Question
        Question q = new Question(
                "Q-TEST-01",
                "What is runtime polymorphism in Java?",
                List.of("Method overloading", "Dynamic method dispatch", "Constructor chaining", "Interface callback"),
                1,
                "Polymorphism",
                Difficulty.MEDIUM,
                "CO-2",
                "Dynamic method dispatch allows JVM to invoke overridden method at runtime."
        );
        questionService.saveOrUpdateQuestion(q);

        Optional<Question> found = questionService.getQuestionById("Q-TEST-01");
        assertTrue(found.isPresent());
        assertEquals("Polymorphism", found.get().getConcept());

        // 2. Update Question
        Question updated = found.get();
        updated.setDifficulty(Difficulty.HARD);
        updated.setQuestionText("Updated: What is runtime polymorphism?");
        questionService.saveOrUpdateQuestion(updated);

        Optional<Question> foundUpdated = questionService.getQuestionById("Q-TEST-01");
        assertTrue(foundUpdated.isPresent());
        assertEquals(Difficulty.HARD, foundUpdated.get().getDifficulty());
        assertEquals("Updated: What is runtime polymorphism?", foundUpdated.get().getQuestionText());

        // 3. Delete Question
        boolean deleted = questionService.deleteQuestion("Q-TEST-01");
        assertTrue(deleted);
        assertTrue(questionService.getQuestionById("Q-TEST-01").isEmpty());
    }

    @Test
    @DisplayName("Instructor: Quiz Scheduling & Configuration Validation")
    public void testInstructorQuizCreation() throws Exception {
        Question q1 = new Question("Q-1", "Prompt 1", List.of("A", "B"), 0, "Inheritance", Difficulty.EASY, "CO-1", "Exp");
        questionRepo.save(q1);

        Quiz quiz = new Quiz(
                "QUIZ-NEW",
                "Advanced Concurrency Midterm",
                "Timed exam",
                10,
                65,
                "instructor_admin",
                List.of("Q-1"),
                "CO-3",
                List.of("Timed exam rules")
        );
        quizService.createQuiz(quiz);

        Optional<Quiz> retrieved = quizService.getQuizById("QUIZ-NEW");
        assertTrue(retrieved.isPresent());
        assertEquals(10, retrieved.get().getDurationMinutes());
        assertEquals(65, retrieved.get().getPassPercentage());

        // Validate rejection of blank title
        assertThrows(ValidationException.class, () -> {
            Quiz invalid = new Quiz(null, "", "Desc", 5, 50, "prof", List.of("Q-1"), "CO-1", List.of());
            quizService.createQuiz(invalid);
        });

        // Validate rejection of 0 duration
        assertThrows(ValidationException.class, () -> {
            Quiz invalid = new Quiz(null, "Valid Title", "Desc", 0, 50, "prof", List.of("Q-1"), "CO-1", List.of());
            quizService.createQuiz(invalid);
        });
    }

    @Test
    @DisplayName("Submission: Automatic Timeout Submission Pipeline")
    public void testAutomaticTimeoutSubmission() throws Exception {
        Question q = new Question("Q-TMO", "Sample prompt", List.of("A", "B", "C", "D"), 2, "Multithreading", Difficulty.EASY, "CO-3", "Exp");
        questionRepo.save(q);

        Quiz quiz = new Quiz("QZ-TIMEOUT", "Timeout Exam", "Desc", 1, 60, "prof", List.of("Q-TMO"), "CO-3", List.of());
        quizRepo.save(quiz);

        User student = new Student("STU-99", "thanushree", "Thanushree", "t@alliance.edu", "s", "h", "CS");

        // Timeout occurs with student answer selected
        Map<Integer, Integer> answers = Map.of(0, 2); // correct answer
        QuizAttempt attempt = resultService.evaluateAndSaveAttempt(
                quiz,
                student,
                List.of(q),
                answers,
                "2026-10-13T10:00:00",
                60,
                "AUTOMATIC_TIMEOUT"
        );

        assertNotNull(attempt);
        assertEquals("AUTOMATIC_TIMEOUT", attempt.getSubmissionType());
        assertEquals(1, attempt.getScore());
        assertEquals(100.0, attempt.getPercentage());
        assertEquals("CORRECT", attempt.getSnapshots().get(0).getStatus());

        // Verify attempt is retrieved from repository
        Optional<QuizAttempt> fetched = attemptRepo.findById(attempt.getId());
        assertTrue(fetched.isPresent());
        assertEquals("AUTOMATIC_TIMEOUT", fetched.get().getSubmissionType());
    }

    @Test
    @DisplayName("Persistence: Data Survives Across Application & Repository Restarts")
    public void testPersistenceAcrossRestart() throws Exception {
        // Save initial entities
        Student student = authService.registerStudent(
                "persist_student",
                "Persist Student",
                "p@ced.alliance.edu.in",
                "Password123",
                "Password123",
                "CS"
        );
        Question question = new Question("Q-PERSIST", "Persistence prompt", List.of("X", "Y"), 0, "Collections & HashMap", Difficulty.MEDIUM, "CO-3", "HashMap");
        questionService.saveOrUpdateQuestion(question);

        Quiz quiz = new Quiz("QUIZ-PERSIST", "Persisted Quiz", "", 15, 60, "instructor_admin", List.of("Q-PERSIST"), "CO-3", List.of());
        quizService.createQuiz(quiz);

        QuizAttempt attempt = resultService.evaluateAndSaveAttempt(
                quiz, student, List.of(question), Map.of(0, 0), "time", 120, "MANUAL"
        );

        // Simulate application restart: instantiate brand new repositories pointing to the same storageService
        UserRepository newUsers = new UserRepository(storageService);
        QuestionRepository newQuestions = new QuestionRepository(storageService);
        QuizRepository newQuizzes = new QuizRepository(storageService);
        AttemptRepository newAttempts = new AttemptRepository(storageService);

        assertTrue(newUsers.existsByUsername("persist_student"), "User must persist across reload");
        assertTrue(newQuestions.findById("Q-PERSIST").isPresent(), "Question must persist across reload");
        assertTrue(newQuizzes.findById("QUIZ-PERSIST").isPresent(), "Quiz must persist across reload");
        assertTrue(newAttempts.findById(attempt.getId()).isPresent(), "Attempt snapshot must persist across reload");
    }
}
