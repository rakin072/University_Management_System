package com.example.ums.controller;

import com.example.ums.model.Admin;
import com.example.ums.model.Student;
import com.example.ums.model.Teacher;
import com.example.ums.repository.AdminRepository;
import com.example.ums.repository.StudentRepository;
import com.example.ums.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        String username = auth.getName();
        var roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("username", username);
        response.put("roles", roles);

        Optional<Admin> admin = adminRepository.findByUsername(username);
        if (admin.isPresent()) {
            response.put("primaryRole", "ADMIN");
            response.put("profile", admin.get());
            return ResponseEntity.ok(response);
        }

        Optional<Teacher> teacher = teacherRepository.findByUsername(username);
        if (teacher.isPresent()) {
            response.put("primaryRole", "TEACHER");
            response.put("profile", teacher.get());
            return ResponseEntity.ok(response);
        }

        Optional<Student> student = studentRepository.findByUsername(username);
        if (student.isPresent()) {
            response.put("primaryRole", "STUDENT");
            response.put("profile", student.get());
            return ResponseEntity.ok(response);
        }

        response.put("primaryRole", "UNKNOWN");
        return ResponseEntity.ok(response);
    }
}
