package com.example.ums.config;

import com.example.ums.model.Admin;
import com.example.ums.model.Student;
import com.example.ums.model.Teacher;
import com.example.ums.repository.AdminRepository;
import com.example.ums.repository.StudentRepository;
import com.example.ums.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepo;
    private final TeacherRepository teacherRepo;
    private final StudentRepository studentRepo;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (adminRepo.findByUsername("admin").isEmpty()) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword(encoder.encode("admin123"));
            admin.setFullName("System Administrator");
            admin.setEmail("admin@univ.edu");
            admin.setOfficeNumber("Room 101");
            adminRepo.save(admin);
        }

        if (teacherRepo.findByUsername("teacher").isEmpty()) {
            Teacher teacher = new Teacher();
            teacher.setUsername("teacher");
            teacher.setPassword(encoder.encode("teacher123"));
            teacher.setFullName("Dr. Alan Turing");
            teacher.setEmail("turing@univ.edu");
            teacher.setDepartment("CSE");
            teacher.setDesignation("Professor");
            teacherRepo.save(teacher);
        }

        if (studentRepo.findByUsername("student").isEmpty()) {
            Student student = new Student();
            student.setUsername("student");
            student.setPassword(encoder.encode("student123"));
            student.setFullName("Ada Lovelace");
            student.setEmail("ada@univ.edu");
            student.setProgram("B.Sc. in CSE");
            student.setBatch(2026);
            studentRepo.save(student);
        }
    }
}
