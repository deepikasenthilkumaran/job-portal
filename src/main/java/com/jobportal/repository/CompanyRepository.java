package com.jobportal.repository;

import com.jobportal.model.Company;
import com.jobportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByOwner(User owner);
}