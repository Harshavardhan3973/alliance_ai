# ALLIANCE UNIVERSITY
### Alliance School of Advanced Computing
**Micro-Project Proposal & Implementation**
**Course:** CS204 Advanced Java Programming & Concurrent Systems

---

## Project Title
**Online Quiz Management System with Synchronized Timer, Question-Based Revision & Concept-Wise Performance Analytics**

### Student Team Members
* **Monisha R**
* **D Thanushree**
* **K Dheeraj Chowdary**
* **Harsha Vardhan E**
* **Kadalinga**

---

## 1.0 Executive Summary & Rationale
Traditional online examination portals merely present a raw numeric score upon exam completion. During a strict, high-pressure timed examination, students' cognitive focus is entirely centered on answering questions within the countdown, leaving them with no opportunity to take notes regarding questions they struggled with.

The **primary innovation of this project** is **Question-Based Revision & Concept-Wise Mistake Analysis**:
1. **Immutable Question Snapshots:** Rather than saving only a score or relying on mutable live question bank pointers, every exam submission captures the *exact questions, multiple choices, correct answer, student response, and explanatory rationale* at the time of submission.
2. **Concept-Wise Performance (HashMap Aggregation):** Every question in the bank is categorized into a specific Java concept (e.g., *Inheritance, Polymorphism, Interfaces, Exception Handling, Multithreading, Collections & HashMap*). Aggregating student attempts with Java `HashMap` highlights weak topics (< 60% accuracy) and produces automated faculty revision recommendations.
3. **Synchronized Multithreaded Quiz Timer:** Concurrently runs on a background `ScheduledExecutorService` thread with millisecond deadline enforcement. When time expires, all current answers are automatically submitted and authoritatively scored without double-submission risk.

---

## 2.0 Course Outcomes Addressed
* **CO-1:** Evolve Java programs incorporating classes, objects, and inheritance concepts (`User` abstract hierarchy, `Student`, `Instructor`).
* **CO-2:** Develop Java programs incorporating polymorphism, interfaces, and packages (`QuestionSnapshot`, `TimerListener`, `CardLayout`, dynamic method dispatch).
* **CO-3:** Create Java programs incorporating exception handling and multithreading (`QuizTimer` concurrency, `ScheduledExecutorService`, `QuizException`, `ValidationException`, `AuthenticationException`, `StorageException`).

---

## 3.0 Project Architecture & Package Structure

```text
online-quiz-management-system/
├── pom.xml                               # Maven Project Descriptor (Java 17 LTS, JUnit 5, Jackson, FlatLaf)
├── README.md                             # Comprehensive project documentation
├── data/                                 # Atomic JSON local file storage
│   ├── users.json                        # Salted PBKDF2 student and faculty records
│   ├── questions.json                    # Question bank with concept and CO tags
│   ├── quizzes.json                      # Examination schedules & rules
│   ├── attempts.json                     # Immutable historical attempt snapshots
│   └── settings.json                     # Institutional configuration & CO mapping
└── src/
    ├── main/
    │   └── java/com/alliance/quizmanagement/
    │       ├── Main.java                 # Desktop Application Bootstrap
    │       ├── model/                    # Data models (User, Student, Instructor, Question, Quiz, etc.)
    │       ├── security/                 # PBKDF2WithHmacSHA256 password security & salt generator
    │       ├── exception/                # Application exception hierarchy (QuizException, etc.)
    │       ├── repository/               # File repositories with thread-safe in-memory caching
    │       ├── service/                  # Business services (Auth, Question, Quiz, Scoring, Storage)
    │       ├── analytics/                # ConceptAnalyticsService utilizing HashMap
    │       ├── util/                     # QuizTimer multithreaded scheduler
    │       └── ui/                       # Swing desktop user interface (20 complete screens)
    └── test/
        └── java/com/alliance/quizmanagement/
            └── QuizManagementTest.java   # Full JUnit 5 test suite
```

---

## 4.0 Demonstration Accounts

| Role | Username | Password | Full Name |
|---|---|---|---|
| **Instructor** | `instructor_admin` | `Password123` | Prof. Rajesh Kumar (Faculty Advisor) |
| **Student** | `harsha_v` | `Password123` | Harsha Vardhan E |
| **Student** | `monisha_r` | `Password123` | Monisha R |
| **Student** | `thanushree_d` | `Password123` | D Thanushree |

---

## 5.0 How to Build and Execute

### Prerequisites
* Java Development Kit (JDK 17 or higher)
* Apache Maven 3.8+

### Execution Commands
```bash
# 1. Run unit test suite
mvn test

# 2. Package into an executable JAR
mvn package

# 3. Launch the Java Swing Desktop Application
java -jar target/online-quiz-management-system-1.0.0.jar
```
