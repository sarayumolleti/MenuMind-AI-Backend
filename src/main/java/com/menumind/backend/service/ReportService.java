package com.menumind.backend.service;

import com.menumind.backend.dto.report.BusinessReportDTO;

import java.time.LocalDate;

/**
 * ReportService
 *
 * Generates comprehensive business reports and CSV exports.
 */
public interface ReportService {

    /**
     * Generates a full business report (JSON) for a given date range.
     */
    BusinessReportDTO generateBusinessReport(LocalDate startDate, LocalDate endDate);

    /**
     * Generates a CSV string of the sales report for export/download.
     */
    String exportSalesReportCsv(LocalDate startDate, LocalDate endDate);

    /**
     * Generates a CSV string of per-dish profit analysis for export/download.
     */
    String exportDishProfitCsv(LocalDate startDate, LocalDate endDate);
}
