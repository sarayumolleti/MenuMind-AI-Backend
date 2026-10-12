package com.menumind.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * MenuItem Entity
 * 
 * Maps to 'menu_items' table.
 * Crucial attributes:
 * - sellingPrice: Menu price charged to customers
 * - estimatedCost: Raw food ingredient preparation cost
 * These two values drive the profit analytics and the BCG Menu Engineering Matrix.
 */
@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "selling_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "estimated_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "is_veg", nullable = false)
    @Builder.Default
    private Boolean isVeg = true;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isVeg == null) this.isVeg = true;
        if (this.isAvailable == null) this.isAvailable = true;
        if (this.isActive == null) this.isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Profit earned on a single unit sold = Selling Price - Estimated Cost
     */
    public BigDecimal getUnitProfit() {
        if (sellingPrice == null || estimatedCost == null) {
            return BigDecimal.ZERO;
        }
        return sellingPrice.subtract(estimatedCost);
    }

    /**
     * Profit Margin percentage = (Unit Profit / Selling Price) * 100
     */
    public BigDecimal getProfitMarginPercentage() {
        if (sellingPrice == null || sellingPrice.compareTo(BigDecimal.ZERO) == 0 || estimatedCost == null) {
            return BigDecimal.ZERO;
        }
        return getUnitProfit()
                .divide(sellingPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
