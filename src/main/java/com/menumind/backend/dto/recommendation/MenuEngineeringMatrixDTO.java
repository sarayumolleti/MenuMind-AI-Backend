package com.menumind.backend.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Menu Engineering Matrix DTO
 * 
 * Based on the BCG Matrix (Boston Consulting Group).
 * Classifies menu items based on Profitability (Margin) and Popularity (Volume).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuEngineeringMatrixDTO {

    // High Profit, High Volume - The best items! Promote them heavily.
    private List<MatrixItemDTO> stars;

    // High Profit, Low Volume - Difficult to sell but highly profitable. Need better placement/marketing.
    private List<MatrixItemDTO> puzzles;

    // Low Profit, High Volume - Popular but low margin. Consider slight price increases or portion control.
    private List<MatrixItemDTO> plowhorses;

    // Low Profit, Low Volume - Losers. Consider removing from the menu entirely.
    private List<MatrixItemDTO> dogs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatrixItemDTO {
        private Long menuItemId;
        private String name;
        private Long totalVolume;
        private java.math.BigDecimal totalProfit;
        private java.math.BigDecimal unitProfit;
    }
}
