package com.example.callyaibackend.repo;

import com.example.callyaibackend.model.MacroDistribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MacroDistributionRepo extends JpaRepository<MacroDistribution, Long> {
    Optional<MacroDistribution> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}