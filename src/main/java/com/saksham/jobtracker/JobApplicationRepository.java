package com.saksham.jobtracker;

import java.sql.Statement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JobApplicationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JobApplicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long findUserId(String username) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?",
                Long.class,
                username
        );
    }

    public List<JobApplication> findAll(Long userId) {
        return jdbcTemplate.query(
                """
                SELECT id, company, role, status
                FROM applications
                WHERE user_id = ?
                ORDER BY id DESC
                """,
                (resultSet, rowNumber) -> new JobApplication(
                        resultSet.getLong("id"),
                        resultSet.getString("company"),
                        resultSet.getString("role"),
                        resultSet.getString("status")
                ),
                userId
        );
    }

    public JobApplication save(
            String company,
            String role,
            Long userId
    ) {
        var keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            var statement = connection.prepareStatement(
                    """
                    INSERT INTO applications
                        (company, role, status, user_id)
                    VALUES (?, ?, ?, ?)
                    """,
                    Statement.RETURN_GENERATED_KEYS
            );

            statement.setString(1, company);
            statement.setString(2, role);
            statement.setString(3, "APPLIED");
            statement.setLong(4, userId);

            return statement;
        }, keyHolder);

        Number id = keyHolder.getKey();

        if (id == null) {
            throw new IllegalStateException(
                    "Database did not return an application ID"
            );
        }

        return new JobApplication(
                id.longValue(), company, role, "APPLIED"
        );
    }

    public boolean deleteById(Long id, Long userId) {
        return jdbcTemplate.update(
                """
                DELETE FROM applications
                WHERE id = ? AND user_id = ?
                """,
                id,
                userId
        ) > 0;
    }

    public boolean updateStatus(
            Long id,
            String status,
            Long userId
    ) {
        return jdbcTemplate.update(
                """
                UPDATE applications
                SET status = ?
                WHERE id = ? AND user_id = ?
                """,
                status,
                id,
                userId
        ) > 0;
    }
}