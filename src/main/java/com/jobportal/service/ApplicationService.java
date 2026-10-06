package com.jobportal.service;

import com.jobportal.dto.Dtos.*;
import com.jobportal.model.*;
import com.jobportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository appRepo;
    private final StatusHistoryRepository historyRepo;
    private final JobRepository jobRepo;
    private final JobService jobService;

    @Transactional
    public ApplicationResponse apply(User candidate, Long jobId, String coverNote) {
        Job job = jobService.find(jobId);
        if (job.getStatus() != JobStatus.OPEN
                || (job.getExpiresAt() != null && job.getExpiresAt().isBefore(LocalDate.now()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This job is closed");
        }
        if (candidate.getResumePath() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload your resume before applying");
        }
        if (appRepo.existsByJobAndCandidate(job, candidate)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already applied to this job");
        }
        Application a = new Application();
        a.setJob(job);
        a.setCandidate(candidate);
        a.setCoverNote(coverNote);
        a.setResumePath(candidate.getResumePath());
        a.setMatchScore(MatchService.score(candidate.getSkills(), job.getRequiredSkills()));
        appRepo.save(a);
        log(a, ApplicationStatus.APPLIED, "Application submitted");
        return toResponse(a);
    }

    public List<ApplicationResponse> mine(User candidate) {
        return appRepo.findByCandidateOrderByAppliedAtDesc(candidate).stream().map(this::toResponse).toList();
    }

    public List<TimelineEntry> timeline(User candidate, Long appId) {
        Application a = find(appId);
        if (!a.getCandidate().getId().equals(candidate.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your application");
        }
        return historyRepo.findByApplicationOrderByChangedAtAsc(a).stream()
                .map(h -> new TimelineEntry(h.getStatus(), h.getNote(), h.getChangedAt())).toList();
    }

    public List<ApplicationResponse> forJob(User recruiter, Long jobId) {
        Job job = jobService.owned(recruiter, jobId);
        return appRepo.findByJobOrderByMatchScoreDesc(job).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ApplicationResponse updateStatus(User recruiter, Long appId, StatusUpdateRequest r) {
        Application a = ownedByRecruiter(recruiter, appId);
        if (a.getStatus() == ApplicationStatus.OFFERED || a.getStatus() == ApplicationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Application is already finalized");
        }
        if (a.getStatus() == r.status()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status is unchanged");
        }
        a.setStatus(r.status());
        appRepo.save(a);
        log(a, r.status(), r.note());
        return toResponse(a);
    }

    public Application ownedByRecruiter(User recruiter, Long appId) {
        Application a = find(appId);
        if (!a.getJob().getCompany().getOwner().getId().equals(recruiter.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your application");
        }
        return a;
    }

    public Map<String, Object> dashboard(User recruiter) {
        List<Job> jobs = jobRepo.findByCompanyOwnerOrderByPostedAtDesc(recruiter);

        Map<String, Long> funnel = new LinkedHashMap<>();
        for (ApplicationStatus s : ApplicationStatus.values()) funnel.put(s.name(), 0L);
        long total = 0;
        for (Object[] row : appRepo.funnel(recruiter)) {
            long c = ((Number) row[1]).longValue();
            funnel.put(((ApplicationStatus) row[0]).name(), c);
            total += c;
        }

        List<Map<String, Object>> perJob = new ArrayList<>();
        for (Object[] row : appRepo.perJob(recruiter)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("jobId", row[0]);
            m.put("jobTitle", row[1]);
            m.put("applications", ((Number) row[2]).longValue());
            m.put("avgMatchScore", row[3] == null ? 0 : Math.round(((Number) row[3]).doubleValue()));
            perJob.add(m);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalJobs", jobs.size());
        out.put("openJobs", jobs.stream().filter(j -> j.getStatus() == JobStatus.OPEN).count());
        out.put("totalApplications", total);
        out.put("funnel", funnel);
        out.put("perJob", perJob);
        return out;
    }

    private Application find(Long id) {
        return appRepo.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private void log(Application a, ApplicationStatus status, String note) {
        StatusHistory h = new StatusHistory();
        h.setApplication(a);
        h.setStatus(status);
        h.setNote(note);
        historyRepo.save(h);
    }

    public ApplicationResponse toResponse(Application a) {
        return new ApplicationResponse(a.getId(), a.getJob().getId(), a.getJob().getTitle(),
                a.getJob().getCompany().getName(), a.getCandidate().getFullName(),
                a.getCandidate().getEmail(), a.getCandidate().getSkills(), a.getStatus(),
                a.getMatchScore(), a.getCoverNote(), a.getResumePath() != null, a.getAppliedAt());
    }
}