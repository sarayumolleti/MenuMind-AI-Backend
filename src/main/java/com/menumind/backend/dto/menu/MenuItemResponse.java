package com.menumind.backend.dto.menu;

import com.menumind.backend.entity.MenuItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Menu Item Response DTO
 * 
 * Exposes dish details along with computed unit profit and margin percentage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemResponse {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal sellingPrice;
    private BigDecimal estimatedCost;
    private BigDecimal unitProfit;
    private BigDecimal profitMarginPercentage;
    private String imageUrl;
    private Boolean isVeg;
    private Boolean isAvailable;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MenuItemResponse fromEntity(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                .categoryName(item.getCategory() != null ? item.getCategory().getName() : null)
                .name(item.getName())
                .description(item.getDescription())
                .sellingPrice(item.getSellingPrice())
                .estimatedCost(item.getEstimatedCost())
                .unitProfit(item.getUnitProfit())
                .profitMarginPercentage(item.getProfitMarginPercentage())
                .imageUrl(item.getImageUrl())
                .isVeg(item.getIsVeg())
                .isAvailable(item.getIsAvailable())
                .isActive(item.getIsActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
