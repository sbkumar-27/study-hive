package com.studyhive.backend.common.security;

import com.studyhive.backend.common.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return userRepository
                .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .map(AppUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user found for " + usernameOrEmail));
    }
}
