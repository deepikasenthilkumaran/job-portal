package com.jobportal.dto;

import com.jobportal.model.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, message = "must be at least 6 characters") String password,
            @NotNull Role role) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, String role, String fullName) {}

    public record CompanyRequest(@NotBlank String name, String description, String website, String location) {}

    public record JobRequest(
            @NotBlank String title,
            @NotBlank String description,
            @NotBlank String location,
            Integer minSalary,
            Integer maxSalary,
            @NotNull EmploymentType employmentType,
            @NotBlank String requiredSkills,
            LocalDate expiresAt) {}

    public record ProfileRequest(String skills, String location) {}

    public record ApplyRequest(String coverNote) {}

    public record StatusUpdateRequest(@NotNull ApplicationStatus status, String note) {}

    public record JobResponse(Long id, String title, String description, String location,
                              Integer minSalary, Integer maxSalary, EmploymentType employmentType,
                              String requiredSkills, JobStatus status, String companyName,
                              LocalDateTime postedAt, LocalDate expiresAt, Integer matchScore,
                              String missingSkills) {}

    public record ApplicationResponse(Long id, Long jobId, String jobTitle, String companyName,
                                      String candidateName, String candidateEmail, String candidateSkills,
                                      ApplicationStatus status, Integer matchScore, String coverNote,
                                      boolean hasResume, LocalDateTime appliedAt) {}

    public record TimelineEntry(ApplicationStatus status, String note, LocalDateTime changedAt) {}
}