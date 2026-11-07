package com.example.ums.unit;

import com.example.ums.model.Admin;
import com.example.ums.model.Student;
import com.example.ums.model.Teacher;
import com.example.ums.repository.AdminRepository;
import com.example.ums.repository.StudentRepository;
import com.example.ums.repository.TeacherRepository;
import com.example.ums.security.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test 5 & 6: Test valid user login details and invalid username.
 * Tests the CustomUserDetailsService which loads users for Spring Security.
 */
@ExtendWith(MockitoExtension.class)
class UserDetailsServiceUnitTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private Admin admin;
    private Teacher teacher;
    private Student student;

    @BeforeEach
    void setUp() {
        admin = new Admin();
        admin.setUsername("admin");
        admin.setPassword("$2a$10$hashedPasswordHere");
        admin.setFullName("System Administrator");
        admin.setEmail("admin@univ.edu");

        teacher = new Teacher();
        teacher.setUsername("drsmith");
        teacher.setPassword("$2a$10$hashedTeacherPwdHere");
        teacher.setFullName("Dr. John Smith");
        teacher.setEmail("smith@univ.edu");
        teacher.setDepartment("CSE");

        student = new Student();
        student.setUsername("ada");
        student.setPassword("$2a$10$hashedStudentPwdHere");
        student.setFullName("Ada Lovelace");
        student.setEmail("ada@univ.edu");
        student.setProgram("B.Sc. in CSE");
    }

    // ─── Unit Test 5: Test valid user login details ───────────────────────────

    @Test
    void testLoadUserByUsername_ValidAdmin_ReturnsUserDetailsWithAdminRole() {
        // Arrange
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        // Assert
        assertNotNull(userDetails);
        assertEquals("admin", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")),
                "Admin user must have ROLE_ADMIN authority");

        verify(adminRepository, times(1)).findByUsername("admin");
        verifyNoInteractions(teacherRepository, studentRepository);
    }

    @Test
    void testLoadUserByUsername_ValidTeacher_ReturnsUserDetailsWithTeacherRole() {
        // Arrange
        when(adminRepository.findByUsername("drsmith")).thenReturn(Optional.empty());
        when(teacherRepository.findByUsername("drsmith")).thenReturn(Optional.of(teacher));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("drsmith");

        // Assert
        assertNotNull(userDetails);
        assertEquals("drsmith", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER")),
                "Teacher user must have ROLE_TEACHER authority");
    }

    @Test
    void testLoadUserByUsername_ValidStudent_ReturnsUserDetailsWithStudentRole() {
        // Arrange
        when(adminRepository.findByUsername("ada")).thenReturn(Optional.empty());
        when(teacherRepository.findByUsername("ada")).thenReturn(Optional.empty());
        when(studentRepository.findByUsername("ada")).thenReturn(Optional.of(student));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("ada");

        // Assert
        assertNotNull(userDetails);
        assertEquals("ada", userDetails.getUsername());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT")),
                "Student user must have ROLE_STUDENT authority");
    }

    // ─── Unit Test 6: Test invalid username ───────────────────────────────────

    @Test
    void testLoadUserByUsername_InvalidUsername_ThrowsUsernameNotFoundException() {
        // Arrange
        String invalidUsername = "ghost_user_does_not_exist";
        when(adminRepository.findByUsername(invalidUsername)).thenReturn(Optional.empty());
        when(teacherRepository.findByUsername(invalidUsername)).thenReturn(Optional.empty());
        when(studentRepository.findByUsername(invalidUsername)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(invalidUsername),
                "Should throw UsernameNotFoundException for a non-existent username"
        );

        assertTrue(exception.getMessage().contains(invalidUsername),
                "Exception message should include the invalid username");

        verify(adminRepository, times(1)).findByUsername(invalidUsername);
        verify(teacherRepository, times(1)).findByUsername(invalidUsername);
        verify(studentRepository, times(1)).findByUsername(invalidUsername);
    }

    @Test
    void testLoadUserByUsername_EmptyUsername_ThrowsUsernameNotFoundException() {
        // Arrange
        when(adminRepository.findByUsername("")).thenReturn(Optional.empty());
        when(teacherRepository.findByUsername("")).thenReturn(Optional.empty());
        when(studentRepository.findByUsername("")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(""),
                "Empty username should also throw UsernameNotFoundException"
        );
    }

    // ─── Additional edge case test ────────────────────────────────────────────

    @Test
    void testLoadUserByUsername_AdminShouldHaveStudentRole() {
        // Arrange
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin");

        // Assert: admin user should have admin-level access
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")),
                "Admin must have ROLE_ADMIN authority");
    }
}

