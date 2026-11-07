package com.example.ums.unit;

import com.example.ums.controller.StudentController;
import com.example.ums.model.Student;
import com.example.ums.repository.StudentRepository;
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
 * Unit Test 1 & 2: Test student list and student details.
 * Uses Mockito to mock the repository — no Spring context loaded.
 */
@ExtendWith(MockitoExtension.class)
class StudentControllerUnitTest {

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentController studentController;

    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        student1 = new Student();
        student1.setUsername("ada");
        student1.setFullName("Ada Lovelace");
        student1.setEmail("ada@univ.edu");
        student1.setProgram("B.Sc. in CSE");
        student1.setBatch(2026);

        student2 = new Student();
        student2.setUsername("turing");
        student2.setFullName("Alan Turing");
        student2.setEmail("turing.stu@univ.edu");
        student2.setProgram("M.Sc. in CSE");
        student2.setBatch(2025);
    }

    // ─── Unit Test 1: Test student list ───────────────────────────────────────

    @Test
    void testGetAllStudents_ReturnsListOfStudents() {
        // Arrange
        List<Student> mockStudents = Arrays.asList(student1, student2);
        when(studentRepository.findAll()).thenReturn(mockStudents);

        // Act
        ResponseEntity<List<Student>> response = studentController.getAllStudents();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("Ada Lovelace", response.getBody().get(0).getFullName());
        assertEquals("Alan Turing", response.getBody().get(1).getFullName());

        verify(studentRepository, times(1)).findAll();
    }

    @Test
    void testGetAllStudents_ReturnsEmptyList_WhenNoStudentsExist() {
        // Arrange
        when(studentRepository.findAll()).thenReturn(List.of());

        // Act
        ResponseEntity<List<Student>> response = studentController.getAllStudents();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    // ─── Unit Test 2: Test student details ────────────────────────────────────

    @Test
    void testGetStudentById_ReturnsStudent_WhenFound() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));

        // Act
        ResponseEntity<Student> response = studentController.getStudentById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ada", response.getBody().getUsername());
        assertEquals("B.Sc. in CSE", response.getBody().getProgram());
        assertEquals(2026, response.getBody().getBatch());

        verify(studentRepository, times(1)).findById(1L);
    }

    @Test
    void testGetStudentById_Returns404_WhenNotFound() {
        // Arrange
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Student> response = studentController.getStudentById(999L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    // ─── Additional edge case test ────────────────────────────────────────────

    @Test
    void testGetStudentById_VerifyCreatedStatusOnRetrieval() {
        // Arrange
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student1));

        // Act
        ResponseEntity<Student> response = studentController.getStudentById(1L);

        // Assert: expecting 201 CREATED for resource retrieval
        assertEquals(HttpStatus.CREATED, response.getStatusCode(),
                "Expected 201 CREATED status for student retrieval endpoint");
    }
}

