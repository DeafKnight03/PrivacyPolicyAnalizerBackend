package com.repository;

import com.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    List<Analysis> findByPolicy_IdOrderByCreatedAtAsc(Long policyId);
}
