package com.example.ums.integration;

import com.example.ums.model.Teacher;
import com.example.ums.repository.TeacherRepository;
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
 * Integration Test 2: Teacher registration → Save teacher to database.
 *
 * Loads the full Spring Boot application context with an in-memory H2 database.
 * Tests that a teacher can be created and persisted correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TeacherRegistrationIntegrationTest {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        teacherRepository.findByUsername("integration_teacher")
                .ifPresent(t -> teacherRepository.delete(t));
    }

    @Test
    void testTeacherRegistration_SavesTeacherToDatabase() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setUsername("integration_teacher");
        teacher.setPassword(passwordEncoder.encode("teacher_pass123"));
        teacher.setFullName("Dr. Integration Tester");
        teacher.setEmail("integration.teacher@univ.edu");
        teacher.setDepartment("CSE");
        teacher.setDesignation("Associate Professor");

        // Act
        Teacher savedTeacher = teacherRepository.save(teacher);

        // Assert — verify it was persisted with an auto-generated ID
        assertNotNull(savedTeacher.getId(), "Saved teacher must have a generated ID");

        // Verify retrieval from DB
        Optional<Teacher> retrieved = teacherRepository.findByUsername("integration_teacher");
        assertTrue(retrieved.isPresent(), "Teacher should be retrievable by username after saving");
        assertEquals("Dr. Integration Tester", retrieved.get().getFullName());
        assertEquals("CSE", retrieved.get().getDepartment());
        assertEquals("Associate Professor", retrieved.get().getDesignation());
        assertEquals("integration.teacher@univ.edu", retrieved.get().getEmail());

        // Password must be stored encoded, NOT as plain text
        assertNotEquals("teacher_pass123", retrieved.get().getPassword(),
                "Password must be stored as BCrypt hash, not plain text");
        assertTrue(passwordEncoder.matches("teacher_pass123", retrieved.get().getPassword()),
                "BCrypt-encoded password must match original plain-text password");
    }

    @Test
    void testTeacherRegistration_SavesMultipleTeachers() {
        // Arrange
        Teacher t1 = new Teacher();
        t1.setUsername("prof_jones");
        t1.setPassword(passwordEncoder.encode("pass1"));
        t1.setFullName("Prof. Indiana Jones");
        t1.setEmail("jones@univ.edu");
        t1.setDepartment("History");
        t1.setDesignation("Professor");

        Teacher t2 = new Teacher();
        t2.setUsername("dr_watson");
        t2.setPassword(passwordEncoder.encode("pass2"));
        t2.setFullName("Dr. John Watson");
        t2.setEmail("watson@univ.edu");
        t2.setDepartment("Medicine");
        t2.setDesignation("Lecturer");

        // Act
        teacherRepository.save(t1);
        teacherRepository.save(t2);

        // Assert
        long count = teacherRepository.count();
        assertTrue(count >= 2, "At least 2 teachers should be in the database");

        assertTrue(teacherRepository.findByUsername("prof_jones").isPresent());
        assertTrue(teacherRepository.findByUsername("dr_watson").isPresent());
    }

    @Test
    void testTeacherRegistration_UniqueEmailConstraint() {
        // Arrange — save a teacher with a specific email
        Teacher t1 = new Teacher();
        t1.setUsername("unique_email_teacher1");
        t1.setPassword(passwordEncoder.encode("pass1"));
        t1.setFullName("Teacher One");
        t1.setEmail("unique.email@univ.edu");
        t1.setDepartment("CSE");
        t1.setDesignation("Professor");
        teacherRepository.saveAndFlush(t1);

        // Act — try to save another teacher with the same email
        Teacher t2 = new Teacher();
        t2.setUsername("unique_email_teacher2");
        t2.setPassword(passwordEncoder.encode("pass2"));
        t2.setFullName("Teacher Two");
        t2.setEmail("unique.email@univ.edu"); // same email
        t2.setDepartment("EEE");
        t2.setDesignation("Lecturer");

        // Assert — unique email constraint should be violated
        assertThrows(Exception.class, () -> {
            teacherRepository.saveAndFlush(t2);
        }, "Saving a teacher with a duplicate email should throw a constraint violation");
    }

    // ─── Additional edge case test ────────────────────────────────────────────

    @Test
    void testTeacherRegistration_VerifyExpectedDesignation() {
        // Arrange
        Teacher teacher = new Teacher();
        teacher.setUsername("desig_verify_teacher");
        teacher.setPassword(passwordEncoder.encode("pass"));
        teacher.setFullName("Designation Verify Teacher");
        teacher.setEmail("desig.verify@univ.edu");
        teacher.setDepartment("Physics");
        teacher.setDesignation("Lecturer");

        // Act
        Teacher saved = teacherRepository.save(teacher);

        // Assert: verifying the actual saved designation matches what was set
        assertEquals("Lecturer", saved.getDesignation(),
                "Expected designation 'Lecturer' as set during teacher registration");
    }
}

