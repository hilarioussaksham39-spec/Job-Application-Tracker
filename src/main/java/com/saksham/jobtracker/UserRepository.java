package com.saksham.jobtracker;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void create(String username, String passwordHash) {
        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash) VALUES (?, ?)",
                username,
                passwordHash
        );
    }
}