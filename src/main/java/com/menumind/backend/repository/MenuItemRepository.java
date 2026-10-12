package com.menumind.backend.repository;

import com.menumind.backend.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MenuItemRepository
 * 
 * Provides database operations and multi-criteria dynamic filtering for Menu Items.
 */
@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByIsActiveTrue();

    List<MenuItem> findByCategoryIdAndIsActiveTrue(Long categoryId);

    List<MenuItem> findByIsActiveTrueAndIsAvailableTrue();

    Boolean existsByNameIgnoreCase(String name);

    /**
     * Multi-criteria search and filter method.
     * Supports filtering by categoryId, veg/non-veg, in-stock availability, and keyword text search.
     */
    @Query("SELECT m FROM MenuItem m WHERE m.isActive = true " +
           "AND (:categoryId IS NULL OR m.category.id = :categoryId) " +
           "AND (:isVeg IS NULL OR m.isVeg = :isVeg) " +
           "AND (:availableOnly IS FALSE OR m.isAvailable = true) " +
           "AND (:keyword IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "     OR LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<MenuItem> searchMenuItems(
            @Param("categoryId") Long categoryId,
            @Param("isVeg") Boolean isVeg,
            @Param("availableOnly") Boolean availableOnly,
            @Param("keyword") String keyword
    );
}
