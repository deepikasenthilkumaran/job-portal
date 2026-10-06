package com.jobportal.service;

import com.jobportal.dto.Dtos.*;
import com.jobportal.model.*;
import com.jobportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepo;
    private final CompanyRepository companyRepo;

    public Page<JobResponse> search(String skill, String location, Integer minSalary,
                                    EmploymentType type, int page, int size) {
        LocalDate today = LocalDate.now();

        Specification<Job> spec = (r, q, cb) -> cb.equal(r.get("status"), JobStatus.OPEN);
        spec = spec.and((r, q, cb) -> cb.or(
                cb.isNull(r.get("expiresAt")),
                cb.greaterThanOrEqualTo(r.get("expiresAt"), today)));

        if (skill != null && !skill.isBlank()) {
            String s = "%" + skill.trim().toLowerCase() + "%";
            spec = spec.and((r, q, cb) -> cb.like(cb.lower(r.get("requiredSkills")), s));
        }
        if (location != null && !location.isBlank()) {
            String l = "%" + location.trim().toLowerCase() + "%";
            spec = spec.and((r, q, cb) -> cb.like(cb.lower(r.get("location")), l));
        }
        if (minSalary != null) {
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("maxSalary"), minSalary));
        }
        if (type != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("employmentType"), type));
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("postedAt").descending());
        return jobRepo.findAll(spec, pageable).map(j -> toResponse(j, null));
    }

    public JobResponse get(Long id) {
        return toResponse(find(id), null);
    }

    @Transactional
    public JobResponse create(User recruiter, JobRequest r) {
        Company company = companyRepo.findByOwner(recruiter).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Create your company profile first"));
        Job job = new Job();
        job.setCompany(company);
        apply(job, r);
        return toResponse(jobRepo.save(job), null);
    }

    @Transactional
    public JobResponse update(User recruiter, Long id, JobRequest r) {
        Job job = owned(recruiter, id);
        apply(job, r);
        return toResponse(jobRepo.save(job), null);
    }

    @Transactional
    public void close(User recruiter, Long id) {
        Job job = owned(recruiter, id);
        job.setStatus(JobStatus.CLOSED);
        jobRepo.save(job);
    }

    public List<JobResponse> mine(User recruiter) {
        return jobRepo.findByCompanyOwnerOrderByPostedAtDesc(recruiter)
                .stream().map(j -> toResponse(j, null)).toList();
    }

    public List<JobResponse> recommended(User candidate) {
        return jobRepo.findByStatus(JobStatus.OPEN).stream()
                .filter(j -> !isExpired(j))
                .map(j -> toResponse(j,
                        MatchService.score(candidate.getSkills(), j.getRequiredSkills()),
                        MatchService.missing(candidate.getSkills(), j.getRequiredSkills())))
                .sorted(Comparator.comparing(JobResponse::matchScore).reversed())
                .limit(10).toList();
    }

    public Job find(Long id) {
        return jobRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    public Job owned(User recruiter, Long id) {
        Job job = find(id);
        if (!job.getCompany().getOwner().getId().equals(recruiter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your job posting");
        }
        return job;
    }

    private void apply(Job job, JobRequest r) {
        if (r.minSalary() != null && r.maxSalary() != null && r.minSalary() > r.maxSalary()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minSalary cannot exceed maxSalary");
        }
        job.setTitle(r.title());
        job.setDescription(r.description());
        job.setLocation(r.location());
        job.setMinSalary(r.minSalary());
        job.setMaxSalary(r.maxSalary());
        job.setEmploymentType(r.employmentType());
        job.setRequiredSkills(MatchService.normalize(r.requiredSkills()));
        job.setExpiresAt(r.expiresAt());
        job.setStatus(isExpired(job) ? JobStatus.CLOSED : JobStatus.OPEN);
    }

    public static boolean isExpired(Job j) {
        return j.getExpiresAt() != null && j.getExpiresAt().isBefore(LocalDate.now());
    }

    public static JobResponse toResponse(Job j, Integer score) {
        return toResponse(j, score, null);
    }

    public static JobResponse toResponse(Job j, Integer score, String missing) {
        JobStatus status = isExpired(j) ? JobStatus.CLOSED : j.getStatus();
        return new JobResponse(j.getId(), j.getTitle(), j.getDescription(), j.getLocation(),
                j.getMinSalary(), j.getMaxSalary(), j.getEmploymentType(), j.getRequiredSkills(),
                status, j.getCompany().getName(), j.getPostedAt(), j.getExpiresAt(), score, missing);
    }
}