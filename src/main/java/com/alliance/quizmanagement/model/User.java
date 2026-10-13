package com.alliance.quizmanagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Abstract base class representing a system user.
 * Demonstrates CO-1: Inheritance, Encapsulation, and Polymorphic User Hierarchies.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "role",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Student.class, name = "STUDENT"),
    @JsonSubTypes.Type(value = Instructor.class, name = "INSTRUCTOR")
})
public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected String username;
    protected String fullName;
    protected String email;
    protected Role role;
    protected String passwordSalt;
    protected String passwordHash;
    protected String department;
    protected boolean active = true;
    protected String createdAt;

    public User() {
    }

    public User(String id, String username, String fullName, String email, Role role,
                String passwordSalt, String passwordHash, String department) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.passwordSalt = passwordSalt;
        this.passwordHash = passwordHash;
        this.department = department;
        this.createdAt = LocalDateTime.now().toString();
    }

    // Abstract method demonstrating polymorphism
    @JsonIgnore
    public abstract String getDisplayRoleTitle();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getPasswordSalt() { return passwordSalt; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
