package com.jobportal.controller;

import com.jobportal.dto.Dtos.*;
import com.jobportal.model.*;
import com.jobportal.repository.CompanyRepository;
import com.jobportal.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;

@RestController
@RequestMapping("/api/recruiter")
@RequiredArgsConstructor
public class RecruiterController {

    private final CurrentUser currentUser;
    private final CompanyRepository companyRepo;
    private final JobService jobService;
    private final ApplicationService appService;
    private final FileStorageService storage;

    @PutMapping("/company")
    public Map<String, Object> saveCompany(@Valid @RequestBody CompanyRequest r, Authentication auth) {
        User u = currentUser.get(auth);
        Company c = companyRepo.findByOwner(u).orElseGet(() -> {
            Company n = new Company();
            n.setOwner(u);
            return n;
        });
        c.setName(r.name());
        c.setDescription(r.description());
        c.setWebsite(r.website());
        c.setLocation(r.location());
        return companyMap(companyRepo.save(c));
    }

    @GetMapping("/company")
    public Map<String, Object> getCompany(Authentication auth) {
        Company c = companyRepo.findByOwner(currentUser.get(auth)).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No company profile yet"));
        return companyMap(c);
    }

    @PostMapping("/jobs")
    public JobResponse createJob(@Valid @RequestBody JobRequest r, Authentication auth) {
        return jobService.create(currentUser.get(auth), r);
    }

    @PutMapping("/jobs/{id}")
    public JobResponse updateJob(@PathVariable Long id, @Valid @RequestBody JobRequest r, Authentication auth) {
        return jobService.update(currentUser.get(auth), id, r);
    }

    @PutMapping("/jobs/{id}/close")
    public Map<String, String> closeJob(@PathVariable Long id, Authentication auth) {
        jobService.close(currentUser.get(auth), id);
        return Map.of("message", "Job closed");
    }

    @GetMapping("/jobs")
    public List<JobResponse> myJobs(Authentication auth) {
        return jobService.mine(currentUser.get(auth));
    }

    @GetMapping("/jobs/{id}/applications")
    public List<ApplicationResponse> applications(@PathVariable Long id, Authentication auth) {
        return appService.forJob(currentUser.get(auth), id);
    }

    @PutMapping("/applications/{id}/status")
    public ApplicationResponse updateStatus(@PathVariable Long id,
                                            @Valid @RequestBody StatusUpdateRequest r, Authentication auth) {
        return appService.updateStatus(currentUser.get(auth), id, r);
    }

    @GetMapping("/applications/{id}/resume")
    public ResponseEntity<Resource> resume(@PathVariable Long id, Authentication auth) {
        Application a = appService.ownedByRecruiter(currentUser.get(auth), id);
        Resource file = storage.load(a.getResumePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + a.getResumePath() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file);
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication auth) {
        return appService.dashboard(currentUser.get(auth));
    }

    private Map<String, Object> companyMap(Company c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("description", c.getDescription());
        m.put("website", c.getWebsite());
        m.put("location", c.getLocation());
        return m;
    }
}