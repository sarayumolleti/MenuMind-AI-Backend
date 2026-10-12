package com.menumind.backend.repository;

import com.menumind.backend.entity.AIInsight;
import com.menumind.backend.entity.InsightType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AIInsightRepository
 * 
 * Provides database queries for AI recommendations and alerts.
 */
@Repository
public interface AIInsightRepository extends JpaRepository<AIInsight, Long> {

    List<AIInsight> findByIsDismissedFalseOrderByCreatedAtDesc();

    List<AIInsight> findByInsightTypeAndIsDismissedFalseOrderByCreatedAtDesc(InsightType insightType);

    void deleteByCreatedAtBefore(LocalDateTime cutoff);
}
