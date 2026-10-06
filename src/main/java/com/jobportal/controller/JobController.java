package com.jobportal.controller;

import com.jobportal.dto.Dtos.JobResponse;
import com.jobportal.model.EmploymentType;
import com.jobportal.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @GetMapping
    public Page<JobResponse> search(@RequestParam(required = false) String skill,
                                    @RequestParam(required = false) String location,
                                    @RequestParam(required = false) Integer minSalary,
                                    @RequestParam(required = false) EmploymentType type,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        return jobService.search(skill, location, minSalary, type, page, Math.min(size, 50));
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable Long id) {
        return jobService.get(id);
    }
}