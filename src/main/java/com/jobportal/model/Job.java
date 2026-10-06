package com.jobportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class Job {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Company company;

    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    private String location;
    private Integer minSalary;
    private Integer maxSalary;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    private String requiredSkills;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.OPEN;

    private LocalDateTime postedAt = LocalDateTime.now();
    private LocalDate expiresAt;
}