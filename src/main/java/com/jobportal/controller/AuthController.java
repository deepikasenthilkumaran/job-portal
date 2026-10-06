package com.jobportal.controller;

import com.jobportal.dto.Dtos.*;
import com.jobportal.model.User;
import com.jobportal.repository.UserRepository;
import com.jobportal.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        if (userRepo.existsByEmail(r.email().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User u = new User();
        u.setFullName(r.fullName());
        u.setEmail(r.email().toLowerCase());
        u.setPassword(encoder.encode(r.password()));
        u.setRole(r.role());
        userRepo.save(u);
        return new AuthResponse(jwt.generate(u.getEmail(), u.getRole().name()), u.getRole().name(), u.getFullName());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        User u = userRepo.findByEmail(r.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!encoder.matches(r.password(), u.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new AuthResponse(jwt.generate(u.getEmail(), u.getRole().name()), u.getRole().name(), u.getFullName());
    }
}