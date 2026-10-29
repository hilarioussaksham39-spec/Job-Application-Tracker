package com.saksham.jobtracker;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    private final JobApplicationRepository repository;

    public JobApplicationController(
            JobApplicationRepository repository
    ) {
        this.repository = repository;
    }

    @GetMapping
    public List<JobApplication> getApplications(Principal principal) {
        Long userId = repository.findUserId(principal.getName());
        return repository.findAll(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobApplication addApplication(
            @RequestBody CreateApplicationRequest request,
            Principal principal
    ) {
        if (request.company() == null || request.company().isBlank()
                || request.role() == null || request.role().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Company and role are required"
            );
        }

        String company = request.company().trim();
        String role = request.role().trim();

        if (company.length() > 150 || role.length() > 150) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Company and role must each be 150 characters or fewer"
            );
        }

        Long userId = repository.findUserId(principal.getName());
        return repository.save(company, role, userId);
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(
            @PathVariable("id") Long id,
            @RequestBody UpdateStatusRequest request,
            Principal principal
    ) {
        List<String> allowedStatuses = List.of(
                "APPLIED", "ASSESSMENT", "INTERVIEW",
                "OFFER", "REJECTED"
        );

        if (request.status() == null
                || !allowedStatuses.contains(request.status())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid application status"
            );
        }

        Long userId = repository.findUserId(principal.getName());

        if (!repository.updateStatus(id, request.status(), userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Application not found"
            );
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApplication(
            @PathVariable("id") Long id,
            Principal principal
    ) {
        Long userId = repository.findUserId(principal.getName());

        if (!repository.deleteById(id, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Application not found"
            );
        }
    }

    public record CreateApplicationRequest(
            String company,
            String role
    ) {
    }

    public record UpdateStatusRequest(String status) {
    }
}