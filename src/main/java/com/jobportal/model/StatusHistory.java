package com.jobportal.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "status_history")
@Getter @Setter @NoArgsConstructor
public class StatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Column(length = 1000)
    private String note;

    private LocalDateTime changedAt = LocalDateTime.now();
}