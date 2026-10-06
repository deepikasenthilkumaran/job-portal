package com.jobportal.repository;

import com.jobportal.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByApplicationOrderByChangedAtAsc(Application application);
}