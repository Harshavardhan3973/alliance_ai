package com.alliance.quizmanagement.model;

/**
 * Concrete Student entity extending User.
 */
public class Student extends User {
    private static final long serialVersionUID = 1L;
    private int attemptedQuizzesCount;

    public Student() {
        super();
        this.role = Role.STUDENT;
    }

    public Student(String id, String username, String fullName, String email,
                   String passwordSalt, String passwordHash, String department) {
        super(id, username, fullName, email, Role.STUDENT, passwordSalt, passwordHash, department);
        this.attemptedQuizzesCount = 0;
    }

    @Override
    public String getDisplayRoleTitle() {
        return "Student Candidate";
    }

    public int getAttemptedQuizzesCount() { return attemptedQuizzesCount; }
    public void setAttemptedQuizzesCount(int count) { this.attemptedQuizzesCount = count; }
}
