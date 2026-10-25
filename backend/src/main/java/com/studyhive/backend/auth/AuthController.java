package com.studyhive.backend.auth;

import com.studyhive.backend.auth.AuthResponse;
import com.studyhive.backend.auth.LoginRequest;
import com.studyhive.backend.auth.RegisterRequest;
import com.studyhive.backend.common.UserDto;
import com.studyhive.backend.common.User;
import com.studyhive.backend.common.exception.ResourceNotFoundException;
import com.studyhive.backend.common.UserRepository;
import com.studyhive.backend.common.security.AppUserPrincipal;
import com.studyhive.backend.auth.AuthService;
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
