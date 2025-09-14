package com.example.myapp.repository;

import com.example.myapp.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
