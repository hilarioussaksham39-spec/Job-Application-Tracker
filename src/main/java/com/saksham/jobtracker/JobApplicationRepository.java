package com.saksham.jobtracker;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JobApplicationRepository {

    private final JdbcTemplate jdbcTemplate;

    public JobApplicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<JobApplication> findAll() {
        String sql = """
                SELECT id, company, role, status
                FROM applications
                ORDER BY id DESC
                """;

        return jdbcTemplate.query(sql, (resultSet, rowNumber) ->
                new JobApplication(
                        resultSet.getLong("id"),
                        resultSet.getString("company"),
                        resultSet.getString("role"),
                        resultSet.getString("status")
                )
        );
    }

    public JobApplication save(String company, String role) {
        String sql = """
                INSERT INTO applications (company, role, status)
                VALUES (?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );

            statement.setString(1, company);
            statement.setString(2, role);
            statement.setString(3, "APPLIED");

            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();

        if (generatedId == null) {
            throw new IllegalStateException(
                    "Database did not return an application ID"
            );
        }

        return new JobApplication(
                generatedId.longValue(),
                company,
                role,
                "APPLIED"
        );
    }
	public boolean deleteById(Long id) {
    int deletedRows = jdbcTemplate.update(
            "DELETE FROM applications WHERE id = ?",
            id
    );

    return deletedRows > 0;
}
public boolean updateStatus(Long id, String status) {
    int updatedRows = jdbcTemplate.update(
            "UPDATE applications SET status = ? WHERE id = ?",
            status,
            id
    );

    return updatedRows > 0;
}
}