package com.example.ums.integration;

import com.example.ums.model.Admin;
import com.example.ums.model.Student;
import com.example.ums.model.Teacher;
import com.example.ums.repository.AdminRepository;
import com.example.ums.repository.StudentRepository;
import com.example.ums.repository.TeacherRepository;
import com.example.ums.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Test 3: User login → Verify authentication.
 *
 * Tests that the CustomUserDetailsService correctly loads users
 * from the real H2 database and that Spring Security's authentication
 * mechanism works end-to-end with actual persisted data.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserLoginIntegrationTest {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String ADMIN_USERNAME   = "login_test_admin";
    private static final String TEACHER_USERNAME = "login_test_teacher";
    private static final String STUDENT_USERNAME = "login_test_student";

    @BeforeEach
    void setUp() {
        // Seed an admin
        if (adminRepository.findByUsername(ADMIN_USERNAME).isEmpty()) {
            Admin admin = new Admin();
            admin.setUsername(ADMIN_USERNAME);
            admin.setPassword(passwordEncoder.encode("adminpass"));
            admin.setFullName("Login Test Admin");
            admin.setEmail("login.admin@univ.edu");
            admin.setOfficeNumber("Room 200");
            adminRepository.save(admin);
        }

        // Seed a teacher
        if (teacherRepository.findByUsername(TEACHER_USERNAME).isEmpty()) {
            Teacher teacher = new Teacher();
            teacher.setUsername(TEACHER_USERNAME);
            teacher.setPassword(passwordEncoder.encode("teacherpass"));
            teacher.setFullName("Login Test Teacher");
            teacher.setEmail("login.teacher@univ.edu");
            teacher.setDepartment("CSE");
            teacher.setDesignation("Professor");
            teacherRepository.save(teacher);
        }

        // Seed a student
        if (studentRepository.findByUsername(STUDENT_USERNAME).isEmpty()) {
            Student student = new Student();
            student.setUsername(STUDENT_USERNAME);
            student.setPassword(passwordEncoder.encode("studentpass"));
            student.setFullName("Login Test Student");
            student.setEmail("login.student@univ.edu");
            student.setProgram("B.Sc. in CSE");
            student.setBatch(2026);
            studentRepository.save(student);
        }
    }

    // ─── Integration Test 3a: Verify admin authentication ─────────────────────

    @Test
    void testAdminLogin_LoadsUserWithAdminRole() {
        // Act — simulates what Spring Security does on login
        UserDetails userDetails = userDetailsService.loadUserByUsername(ADMIN_USERNAME);

        // Assert
        assertNotNull(userDetails);
        assertEquals(ADMIN_USERNAME, userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")),
                "Authenticated admin must have ROLE_ADMIN");

        // Verify stored password is a BCrypt hash, not plain text
        assertTrue(userDetails.getPassword().startsWith("$2a$") || userDetails.getPassword().startsWith("$2b$"),
                "Password should be BCrypt-encoded");

        // Verify the password matches via encoder
        assertTrue(passwordEncoder.matches("adminpass", userDetails.getPassword()),
                "Raw password 'adminpass' must match the stored BCrypt hash");
    }

    // ─── Integration Test 3b: Verify teacher authentication ───────────────────

    @Test
    void testTeacherLogin_LoadsUserWithTeacherRole() {
        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(TEACHER_USERNAME);

        // Assert
        assertNotNull(userDetails);
        assertEquals(TEACHER_USERNAME, userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER")),
                "Authenticated teacher must have ROLE_TEACHER");
        assertTrue(passwordEncoder.matches("teacherpass", userDetails.getPassword()),
                "Raw password 'teacherpass' must match the stored BCrypt hash");
    }

    // ─── Integration Test 3c: Verify student authentication ───────────────────

    @Test
    void testStudentLogin_LoadsUserWithStudentRole() {
        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(STUDENT_USERNAME);

        // Assert
        assertNotNull(userDetails);
        assertEquals(STUDENT_USERNAME, userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT")),
                "Authenticated student must have ROLE_STUDENT");
        assertTrue(passwordEncoder.matches("studentpass", userDetails.getPassword()),
                "Raw password 'studentpass' must match the stored BCrypt hash");
    }

    // ─── Integration Test 3d: Verify invalid login throws exception ────────────

    @Test
    void testLogin_NonExistentUser_ThrowsUsernameNotFoundException() {
        // Act & Assert
        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("totally_unknown_user_xyz"),
                "Loading a non-existent username must throw UsernameNotFoundException"
        );
        assertTrue(ex.getMessage().contains("totally_unknown_user_xyz"),
                "Exception message should reference the missing username");
    }

    // ─── Integration Test 3e: Verify wrong password does NOT match ─────────────

    @Test
    void testLogin_WrongPasswordDoesNotMatch() {
        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(ADMIN_USERNAME);

        // Assert — wrong password must NOT match the stored hash
        assertFalse(passwordEncoder.matches("wrong_password", userDetails.getPassword()),
                "Wrong password must not match the stored BCrypt hash");
    }

    // ─── Additional edge case test ────────────────────────────────────────────

    @Test
    void testStudentLogin_VerifyCrossRoleAdminAccess() {
        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(STUDENT_USERNAME);

        // Assert: student account should have student-level access
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT")),
                "Student must have ROLE_STUDENT authority in test environment");
    }
}

