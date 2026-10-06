package com.jobportal.controller;

import com.jobportal.dto.Dtos.*;
import com.jobportal.model.User;
import com.jobportal.repository.UserRepository;
import com.jobportal.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/candidate")
@RequiredArgsConstructor
public class CandidateController {

    private final CurrentUser currentUser;
    private final UserRepository userRepo;
    private final FileStorageService storage;
    private final ApplicationService appService;
    private final JobService jobService;

    @PutMapping("/profile")
    public Map<String, String> updateProfile(@RequestBody ProfileRequest r, Authentication auth) {
        User u = currentUser.get(auth);
        u.setSkills(MatchService.normalize(r.skills()));
        u.setLocation(r.location());
        userRepo.save(u);
        return Map.of("skills", u.getSkills() == null ? "" : u.getSkills(),
                "location", u.getLocation() == null ? "" : u.getLocation());
    }

    @PostMapping("/resume")
    public Map<String, String> uploadResume(@RequestParam("file") MultipartFile file, Authentication auth) {
        User u = currentUser.get(auth);
        u.setResumePath(storage.save(file, u.getId()));
        userRepo.save(u);
        return Map.of("message", "Resume uploaded", "file", u.getResumePath());
    }

    @GetMapping("/jobs/recommended")
    public List<JobResponse> recommended(Authentication auth) {
        return jobService.recommended(currentUser.get(auth));
    }

    @PostMapping("/jobs/{jobId}/apply")
    public ApplicationResponse apply(@PathVariable Long jobId,
                                     @RequestBody(required = false) ApplyRequest r, Authentication auth) {
        return appService.apply(currentUser.get(auth), jobId, r == null ? null : r.coverNote());
    }

    @GetMapping("/applications")
    public List<ApplicationResponse> myApplications(Authentication auth) {
        return appService.mine(currentUser.get(auth));
    }

    @GetMapping("/applications/{id}/timeline")
    public List<TimelineEntry> timeline(@PathVariable Long id, Authentication auth) {
        return appService.timeline(currentUser.get(auth), id);
    }
}