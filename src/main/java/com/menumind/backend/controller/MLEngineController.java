package com.menumind.backend.controller;

import com.menumind.backend.dto.common.ApiResponse;
import com.menumind.backend.entity.AIInsight;
import com.menumind.backend.entity.DemandForecast;
import com.menumind.backend.service.MLEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MLEngineController
 *
 * REST API for Java-native Machine Learning features: Demand Forecasting and Anomaly Detection.
 * Access restricted to MANAGER and ADMIN roles.
 */
@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN')")
public class MLEngineController {

    private final MLEngineService mlEngineService;

    // -------------------------------------------------------
    // DEMAND FORECASTING
    // -------------------------------------------------------

    @PostMapping("/forecast/{menuItemId}")
    public ResponseEntity<ApiResponse<List<DemandForecast>>> generateForecast(
            @PathVariable Long menuItemId,
            @RequestParam(defaultValue = "7") int days) {
        List<DemandForecast> forecasts = mlEngineService.generateDemandForecast(menuItemId, days);
        return ResponseEntity.ok(ApiResponse.success("Demand forecast generated successfully", forecasts));
    }

    @GetMapping("/forecast/{menuItemId}")
    public ResponseEntity<ApiResponse<List<DemandForecast>>> getForecast(@PathVariable Long menuItemId) {
        List<DemandForecast> forecasts = mlEngineService.getForecast(menuItemId);
        return ResponseEntity.ok(ApiResponse.success("Demand forecast retrieved", forecasts));
    }

    // -------------------------------------------------------
    // AI INSIGHTS & ANOMALY DETECTION
    // -------------------------------------------------------

    @PostMapping("/insights/scan")
    public ResponseEntity<ApiResponse<Void>> triggerAnomalyScan() {
        mlEngineService.detectSalesAnomalies();
        return ResponseEntity.ok(ApiResponse.success("Anomaly scan completed successfully"));
    }

    @GetMapping("/insights")
    public ResponseEntity<ApiResponse<List<AIInsight>>> getActiveInsights() {
        List<AIInsight> insights = mlEngineService.getActiveInsights();
        return ResponseEntity.ok(ApiResponse.success("AI Insights retrieved", insights));
    }

    @PatchMapping("/insights/{insightId}/dismiss")
    public ResponseEntity<ApiResponse<Void>> dismissInsight(@PathVariable Long insightId) {
        mlEngineService.dismissInsight(insightId);
        return ResponseEntity.ok(ApiResponse.success("AI Insight dismissed successfully"));
    }
}
