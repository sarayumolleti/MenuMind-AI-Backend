package com.menumind.backend.dto.recommendation;

import com.menumind.backend.dto.menu.MenuItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Recommendation Item DTO
 * 
 * Used for customer-facing AI recommendations (Cross-sell, Up-sell).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationItemDTO {

    private MenuItemResponse menuItem;
    private String recommendationReason; // e.g., "Frequently bought together", "Popular in Starters"
    private Double matchScore;
}
