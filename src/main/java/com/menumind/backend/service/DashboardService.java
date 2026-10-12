package com.menumind.backend.service;

import com.menumind.backend.dto.dashboard.DashboardSummaryDTO;

/**
 * DashboardService
 *
 * Provides aggregated business data for the manager/admin dashboard UI.
 */
public interface DashboardService {

    /**
     * Gets a complete snapshot for the dashboard view.
     */
    DashboardSummaryDTO getDashboardSummary();
}
