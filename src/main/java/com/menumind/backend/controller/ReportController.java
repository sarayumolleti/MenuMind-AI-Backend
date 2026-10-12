package com.menumind.backend.controller;

import com.menumind.backend.dto.common.ApiResponse;
import com.menumind.backend.dto.report.BusinessReportDTO;
import com.menumind.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * ReportController
 *
 * REST API for business reports and CSV exports.
 * All endpoints are restricted to MANAGER and ADMIN roles.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN')")
public class ReportController {

    private final ReportService reportService;

    // -------------------------------------------------------
    // JSON Business Report
    // -------------------------------------------------------

    @GetMapping("/business")
    public ResponseEntity<ApiResponse<BusinessReportDTO>> getBusinessReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        BusinessReportDTO report = reportService.generateBusinessReport(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Business report generated", report));
    }

    // -------------------------------------------------------
    // CSV Exports (Downloadable Files)
    // -------------------------------------------------------

    @GetMapping("/export/sales")
    public ResponseEntity<byte[]> exportSalesCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        String csv = reportService.exportSalesReportCsv(startDate, endDate);
        String filename = "menumind_sales_" + startDate + "_to_" + endDate + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }

    @GetMapping("/export/dish-profit")
    public ResponseEntity<byte[]> exportDishProfitCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        String csv = reportService.exportDishProfitCsv(startDate, endDate);
        String filename = "menumind_dish_profit_" + startDate + "_to_" + endDate + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.getBytes());
    }
}
