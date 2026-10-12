package com.menumind.backend.service;

import com.menumind.backend.dto.recommendation.MenuEngineeringMatrixDTO;
import com.menumind.backend.dto.recommendation.RecommendationItemDTO;

import java.util.List;

/**
 * RecommendationService
 *
 * Implements AI/ML recommendations for both Customers and Managers.
 */
public interface RecommendationService {

    /**
     * Cross-sell recommendations (Collaborative Filtering).
     * Recommends dishes frequently bought in the same order as the target item.
     * Useful for "Customers who bought X also bought Y" prompts.
     */
    List<RecommendationItemDTO> getCrossSellRecommendations(Long menuItemId);

    /**
     * Personalized recommendations based on the user's past ordering behavior.
     * Recommends popular items from their favorite categories.
     */
    List<RecommendationItemDTO> getPersonalizedRecommendations(Long customerId);

    /**
     * Menu Engineering Matrix (BCG Matrix).
     * Classifies all active menu items into Stars, Puzzles, Plowhorses, and Dogs
     * based on their relative popularity (Volume) and profitability (Unit Margin).
     * Used exclusively by Managers to optimize the menu.
     */
    MenuEngineeringMatrixDTO getMenuEngineeringMatrix();
}
