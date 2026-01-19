package com.japanesestudy.app.controller;

import com.japanesestudy.app.dto.auth.AuthDtos.*;
import com.japanesestudy.app.entity.User;
import com.japanesestudy.app.entity.Role;
import com.japanesestudy.app.repository.UserRepository;
import com.japanesestudy.app.security.JwtUtils;
import com.japanesestudy.app.security.service.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.japanesestudy.app.util.Utils.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList();
            return ok(new JwtResponse(jwt, userDetails.getId(), userDetails.getUsername(), roles));
        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            return unauthorized("Invalid username or password");
        } catch (RuntimeException e) {
            log.error("Login failed", e);
            return error("Login failed: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        try {
            if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                return error("Username is already taken!");
            }
            User user = new User(signUpRequest.getUsername(), encoder.encode(signUpRequest.getPassword()), Role.USER);
            userRepository.save(user);
            return message("User registered successfully!");
        } catch (RuntimeException e) {
            log.error("Signup failed", e);
            return error("Signup failed: " + e.getMessage());
        }
    }
}
