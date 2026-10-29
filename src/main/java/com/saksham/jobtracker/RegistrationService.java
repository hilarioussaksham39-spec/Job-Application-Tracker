package com.saksham.jobtracker;

import java.nio.charset.StandardCharsets;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegistrationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(
            UserRepository repository,
            PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(String username, String password) {
        if (username == null
                || !username.matches("[A-Za-z0-9_]{3,50}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username must contain 3–50 letters, numbers or underscores"
            );
        }

        if (password == null
                || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must be at least 12 characters and at most 72 UTF-8 bytes"
            );
        }

        String passwordHash = passwordEncoder.encode(password);

        try {
            repository.create(username, passwordHash);
        } catch (DuplicateKeyException error) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username is already taken"
            );
        }
    }
}