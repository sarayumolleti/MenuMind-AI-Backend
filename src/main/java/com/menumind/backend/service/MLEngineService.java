package com.menumind.backend.service;

import com.menumind.backend.entity.AIInsight;
import com.menumind.backend.entity.DemandForecast;

import java.util.List;

/**
 * MLEngineService
 *
 * Implements Java-native Machine Learning algorithms for:
 * 1. Demand Forecasting (Simple Linear Regression / Time Series)
 * 2. Anomaly Detection (Statistical Z-Score analysis for spikes/drops)
 */
public interface MLEngineService {

    /**
     * Forecasts demand for a specific menu item for the next N days.
     * Uses historical data and linear regression.
     */
    List<DemandForecast> generateDemandForecast(Long menuItemId, int forecastDays);

    /**
     * Retrieves the most recent demand forecast for a menu item.
     */
    List<DemandForecast> getForecast(Long menuItemId);

    /**
     * Scans recent sales data across all items to detect statistical anomalies.
     * Generates AIInsights when significant spikes or drops are found.
     */
    void detectSalesAnomalies();

    /**
     * Retrieves active (non-dismissed) AI Insights.
     */
    List<AIInsight> getActiveInsights();

    /**
     * Dismisses an AI Insight.
     */
    void dismissInsight(Long insightId);
}
