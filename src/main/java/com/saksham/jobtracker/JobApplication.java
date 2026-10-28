package com.saksham.jobtracker;

public record JobApplication(
        Long id,
        String company,
        String role,
        String status
) {
}
