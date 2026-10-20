package com.studyhive.backend.service;

import com.studyhive.backend.dto.auth.AuthResponse;
import com.studyhive.backend.dto.auth.LoginRequest;
import com.studyhive.backend.dto.auth.RegisterRequest;
import com.studyhive.backend.dto.user.UserDto;
import com.studyhive.backend.entity.AccountStatus;
import com.studyhive.backend.entity.GlobalRole;
import com.studyhive.backend.entity.User;
import com.studyhive.backend.exception.ConflictException;
import com.studyhive.backend.repository.UserRepository;
import com.studyhive.backend.security.AppUserPrincipal;
import com.studyhive.backend.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setGlobalRole(GlobalRole.USER);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user = userRepository.save(user);

        String token = jwtService.generateToken(new AppUserPrincipal(user));
        return new AuthResponse(token, UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByUsernameOrEmail(request.usernameOrEmail(), request.usernameOrEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid username/email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username/email or password");
        }
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BadCredentialsException("This account has been disabled");
        }

        String token = jwtService.generateToken(new AppUserPrincipal(user));
        return new AuthResponse(token, UserDto.from(user));
    }
}
