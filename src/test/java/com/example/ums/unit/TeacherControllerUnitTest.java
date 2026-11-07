package com.example.ums.unit;

import com.example.ums.controller.TeacherController;
import com.example.ums.model.Teacher;
import com.example.ums.repository.TeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test 3 & 4: Test teacher list and teacher details.
 * Uses Mockito to mock the repository — no Spring context loaded.
 */
@ExtendWith(MockitoExtension.class)
class TeacherControllerUnitTest {

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TeacherController teacherController;

    private Teacher teacher1;
    private Teacher teacher2;

    @BeforeEach
    void setUp() {
        teacher1 = new Teacher();
        teacher1.setUsername("drsmith");
        teacher1.setFullName("Dr. John Smith");
        teacher1.setEmail("smith@univ.edu");
        teacher1.setDepartment("CSE");
        teacher1.setDesignation("Professor");

        teacher2 = new Teacher();
        teacher2.setUsername("drjane");
        teacher2.setFullName("Dr. Jane Doe");
        teacher2.setEmail("jane@univ.edu");
        teacher2.setDepartment("EEE");
        teacher2.setDesignation("Associate Professor");
    }

    // ─── Unit Test 3: Test teacher list ───────────────────────────────────────

    @Test
    void testGetAllTeachers_ReturnsListOfTeachers() {
        // Arrange
        List<Teacher> mockTeachers = Arrays.asList(teacher1, teacher2);
        when(teacherRepository.findAll()).thenReturn(mockTeachers);

        // Act
        ResponseEntity<List<Teacher>> response = teacherController.getAllTeachers();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Dr. John Smith", response.getBody().get(0).getFullName());
        assertEquals("CSE", response.getBody().get(0).getDepartment());
        assertEquals("EEE", response.getBody().get(1).getDepartment());

        verify(teacherRepository, times(1)).findAll();
    }

    @Test
    void testGetAllTeachers_ReturnsEmptyList_WhenNoTeachersExist() {
        // Arrange
        when(teacherRepository.findAll()).thenReturn(List.of());

        // Act
        ResponseEntity<List<Teacher>> response = teacherController.getAllTeachers();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isEmpty());
    }

    // ─── Unit Test 4: Test teacher details ────────────────────────────────────

    @Test
    void testGetTeacherById_ReturnsTeacher_WhenFound() {
        // Arrange
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher1));

        // Act
        ResponseEntity<Teacher> response = teacherController.getTeacherById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("drsmith", response.getBody().getUsername());
        assertEquals("Professor", response.getBody().getDesignation());
        assertEquals("CSE", response.getBody().getDepartment());

        verify(teacherRepository, times(1)).findById(1L);
    }

    @Test
    void testGetTeacherById_Returns404_WhenNotFound() {
        // Arrange
        when(teacherRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Teacher> response = teacherController.getTeacherById(999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    // ─── Additional edge case test ────────────────────────────────────────────

    @Test
    void testGetTeacherById_VerifyDepartmentFromDataSource() {
        // Arrange
        when(teacherRepository.findById(1L)).thenReturn(Optional.of(teacher1));

        // Act
        ResponseEntity<Teacher> response = teacherController.getTeacherById(1L);

        // Assert: verifying expected department from data source configuration
        assertEquals("CSE", response.getBody().getDepartment(),
                "Expected department 'CSE' from teacher data source");
    }
}

