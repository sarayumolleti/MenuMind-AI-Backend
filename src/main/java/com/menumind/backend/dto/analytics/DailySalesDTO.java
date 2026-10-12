package com.menumind.backend.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Daily Sales DTO
 *
 * One data point in a time-series of daily revenue + order count.
 * The frontend uses this to render line charts / bar charts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySalesDTO {

    private String date;             // ISO date string: "2026-10-09"
    private Long orderCount;
    private BigDecimal revenue;
}
