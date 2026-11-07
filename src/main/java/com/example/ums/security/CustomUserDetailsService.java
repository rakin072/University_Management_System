package com.example.ums.security;

import com.example.ums.repository.AdminRepository;
import com.example.ums.repository.StudentRepository;
import com.example.ums.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var admin = adminRepository.findByUsername(username);
        if (admin.isPresent()) {
            return User.withUsername(admin.get().getUsername())
                    .password(admin.get().getPassword())
                    .roles("ADMIN")
                    .build();
        }

        var teacher = teacherRepository.findByUsername(username);
        if (teacher.isPresent()) {
            return User.withUsername(teacher.get().getUsername())
                    .password(teacher.get().getPassword())
                    .roles("TEACHER")
                    .build();
        }

        var student = studentRepository.findByUsername(username);
        if (student.isPresent()) {
            return User.withUsername(student.get().getUsername())
                    .password(student.get().getPassword())
                    .roles("STUDENT")
                    .build();
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}
