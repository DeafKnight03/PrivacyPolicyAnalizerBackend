package com.repository;

import com.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
