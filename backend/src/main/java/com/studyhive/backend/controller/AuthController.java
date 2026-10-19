package com.studyhive.backend.controller;

import com.studyhive.backend.dto.auth.AuthResponse;
import com.studyhive.backend.dto.auth.LoginRequest;
import com.studyhive.backend.dto.auth.RegisterRequest;
import com.studyhive.backend.dto.user.UserDto;
import com.studyhive.backend.entity.User;
import com.studyhive.backend.exception.ResourceNotFoundException;
import com.studyhive.backend.repository.UserRepository;
import com.studyhive.backend.security.AppUserPrincipal;
import com.studyhive.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal AppUserPrincipal principal) {
        User user = userRepository
                .findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(UserDto.from(user));
    }
}
