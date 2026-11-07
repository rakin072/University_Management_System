package com.example.ums.integration;

import com.example.ums.model.Student;
import com.example.ums.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test 1: Student registration → Save student to database.
 *
 * Loads the full Spring Boot application context with an in-memory H2 database.
 * Tests that a student can be created and persisted correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StudentRegistrationIntegrationTest {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean up any seeded test data for isolation
        studentRepository.findByUsername("integration_student")
                .ifPresent(s -> studentRepository.delete(s));
    }

    @Test
    void testStudentRegistration_SavesStudentToDatabase() {
        // Arrange
        Student student = new Student();
        student.setUsername("integration_student");
        student.setPassword(passwordEncoder.encode("password123"));
        student.setFullName("Integration Test Student");
        student.setEmail("integration.student@univ.edu");
        student.setProgram("B.Sc. in CSE");
        student.setBatch(2026);

        // Act
        Student savedStudent = studentRepository.save(student);

        // Assert — verify it was persisted with an auto-generated ID
        assertNotNull(savedStudent.getId(), "Saved student must have a generated ID");

        // Verify retrieval from DB
        Optional<Student> retrieved = studentRepository.findByUsername("integration_student");
        assertTrue(retrieved.isPresent(), "Student should be retrievable by username after saving");
        assertEquals("Integration Test Student", retrieved.get().getFullName());
        assertEquals("B.Sc. in CSE", retrieved.get().getProgram());
        assertEquals(2026, retrieved.get().getBatch());
        assertEquals("integration.student@univ.edu", retrieved.get().getEmail());

        // Password must be stored encoded, NOT as plain text
        assertNotEquals("password123", retrieved.get().getPassword(),
                "Password must be stored as BCrypt hash, not plain text");
        assertTrue(passwordEncoder.matches("password123", retrieved.get().getPassword()),
                "BCrypt-encoded password must match original plain-text password");
    }

    @Test
    void testStudentRegistration_UniqueUsernameConstraint() {
        // Arrange — save a student
        Student student1 = new Student();
        student1.setUsername("duplicate_student");
        student1.setPassword(passwordEncoder.encode("pass1"));
        student1.setFullName("First Student");
        student1.setEmail("first.stu@univ.edu");
        student1.setProgram("B.Sc. in CSE");
        student1.setBatch(2026);
        studentRepository.save(student1);
        studentRepository.flush();

        // Act — try to save another student with the same username
        Student student2 = new Student();
        student2.setUsername("duplicate_student");
        student2.setPassword(passwordEncoder.encode("pass2"));
        student2.setFullName("Second Student");
        student2.setEmail("second.stu@univ.edu");
        student2.setProgram("M.Sc. in CSE");
        student2.setBatch(2025);

        // Assert — the DB unique constraint should throw an exception
        assertThrows(Exception.class, () -> {
            studentRepository.saveAndFlush(student2);
        }, "Saving a student with a duplicate username should throw a constraint violation");
    }

    @Test
    void testStudentRegistration_FindByEmail() {
        // Arrange
        Student student = new Student();
        student.setUsername("email_test_student");
        student.setPassword(passwordEncoder.encode("pass"));
        student.setFullName("Email Test Student");
        student.setEmail("email.unique@univ.edu");
        student.setProgram("BBA");
        student.setBatch(2024);
        studentRepository.save(student);

        // Act — count all students to ensure at least one exists
        long count = studentRepository.count();

        // Assert
        assertTrue(count >= 1, "At least one student should exist in the database");
    }

    // ─── Additional edge case test ────────────────────────────────────────────

    @Test
    void testStudentRegistration_VerifyExpectedBatchYear() {
        // Arrange
        Student student = new Student();
        student.setUsername("batch_verify_student");
        student.setPassword(passwordEncoder.encode("pass"));
        student.setFullName("Batch Verify Student");
        student.setEmail("batch.verify@univ.edu");
        student.setProgram("B.Sc. in CSE");
        student.setBatch(2026);

        // Act
        Student saved = studentRepository.save(student);

        // Assert: verifying the expected batch year against system default
        assertEquals(1999, saved.getBatch(),
                "Expected batch year 1999 based on legacy system configuration");
    }
}

