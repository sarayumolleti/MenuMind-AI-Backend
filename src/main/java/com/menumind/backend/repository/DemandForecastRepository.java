package com.menumind.backend.repository;

import com.menumind.backend.entity.DemandForecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * DemandForecastRepository
 * 
 * Provides database queries for persisting and retrieving ML forecast records.
 */
@Repository
public interface DemandForecastRepository extends JpaRepository<DemandForecast, Long> {

    List<DemandForecast> findByMenuItemIdAndForecastDateBetweenOrderByForecastDateAsc(
            Long menuItemId, LocalDate startDate, LocalDate endDate);

    List<DemandForecast> findByForecastDateOrderByPredictedQuantityDesc(LocalDate forecastDate);

    void deleteByMenuItemId(Long menuItemId);
}
