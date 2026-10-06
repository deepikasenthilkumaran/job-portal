package com.jobportal.repository;

import com.jobportal.model.Job;
import com.jobportal.model.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    List<Job> findByCompanyOwnerOrderByPostedAtDesc(User owner);

    List<Job> findByStatus(com.jobportal.model.JobStatus status);

    @Modifying
    @Query("update Job j set j.status = com.jobportal.model.JobStatus.CLOSED " +
            "where j.status = com.jobportal.model.JobStatus.OPEN and j.expiresAt < :today")
    int closeExpired(@Param("today") LocalDate today);
}