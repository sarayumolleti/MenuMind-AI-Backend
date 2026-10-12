package com.menumind.backend.dto.menu;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Menu Item Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemRequest {

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Dish name is required")
    @Size(min = 2, max = 150, message = "Dish name must be between 2 and 150 characters")
    private String name;

    private String description;

    @NotNull(message = "Selling price is required")
    @DecimalMin(value = "0.01", message = "Selling price must be greater than zero")
    private BigDecimal sellingPrice;

    @NotNull(message = "Estimated cost is required")
    @DecimalMin(value = "0.00", message = "Estimated cost cannot be negative")
    private BigDecimal estimatedCost;

    private String imageUrl;

    @Builder.Default
    private Boolean isVeg = true;

    @Builder.Default
    private Boolean isAvailable = true;

    @Builder.Default
    private Boolean isActive = true;
}
