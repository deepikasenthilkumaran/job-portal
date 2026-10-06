package com.jobportal.repository;

import com.jobportal.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    boolean existsByJobAndCandidate(Job job, User candidate);

    List<Application> findByCandidateOrderByAppliedAtDesc(User candidate);

    List<Application> findByJobOrderByMatchScoreDesc(Job job);

    @Query("select a.status, count(a) from Application a join a.job j " +
            "where j.company.owner = :owner group by a.status")
    List<Object[]> funnel(@Param("owner") User owner);

    @Query("select j.id, j.title, count(a), avg(a.matchScore) from Application a join a.job j " +
            "where j.company.owner = :owner group by j.id, j.title")
    List<Object[]> perJob(@Param("owner") User owner);
}