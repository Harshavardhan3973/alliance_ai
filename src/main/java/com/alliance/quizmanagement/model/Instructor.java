package com.alliance.quizmanagement.model;

/**
 * Concrete Instructor entity extending User.
 */
public class Instructor extends User {
    private static final long serialVersionUID = 1L;
    private String designation;
    private int createdQuizzesCount;

    public Instructor() {
        super();
        this.role = Role.INSTRUCTOR;
    }

    public Instructor(String id, String username, String fullName, String email,
                      String passwordSalt, String passwordHash, String department, String designation) {
        super(id, username, fullName, email, Role.INSTRUCTOR, passwordSalt, passwordHash, department);
        this.designation = designation;
        this.createdQuizzesCount = 0;
    }

    @Override
    public String getDisplayRoleTitle() {
        return "Course Instructor (" + (designation != null ? designation : "Faculty") + ")";
    }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public int getCreatedQuizzesCount() { return createdQuizzesCount; }
    public void setCreatedQuizzesCount(int count) { this.createdQuizzesCount = count; }
}
