package com.metrics.exportservice.repository;

import com.metrics.exportservice.entity.Override;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OverrideRepository extends JpaRepository<Override, Long> {
    Optional<Override> findByOverrideName(String overrideName);
}
