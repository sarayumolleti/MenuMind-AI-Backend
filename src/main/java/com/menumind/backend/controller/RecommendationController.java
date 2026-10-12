package com.menumind.backend.controller;

import com.menumind.backend.dto.common.ApiResponse;
import com.menumind.backend.dto.recommendation.MenuEngineeringMatrixDTO;
import com.menumind.backend.dto.recommendation.RecommendationItemDTO;
import com.menumind.backend.security.UserPrincipal;
import com.menumind.backend.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RecommendationController
 *
 * REST API for AI-powered menu recommendations and business intelligence.
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    // -------------------------------------------------------
    // Customer-Facing Endpoints
    // -------------------------------------------------------
    
    @GetMapping("/cross-sell/{menuItemId}")
    public ResponseEntity<ApiResponse<List<RecommendationItemDTO>>> getCrossSell(
            @PathVariable Long menuItemId) {
        List<RecommendationItemDTO> recs = recommendationService.getCrossSellRecommendations(menuItemId);
        return ResponseEntity.ok(ApiResponse.success("Cross-sell recommendations retrieved", recs));
    }

    @GetMapping("/for-me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<RecommendationItemDTO>>> getPersonalized(
            @AuthenticationPrincipal UserPrincipal currentUser) {
        List<RecommendationItemDTO> recs = recommendationService.getPersonalizedRecommendations(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Personalized recommendations retrieved", recs));
    }

    // -------------------------------------------------------
    // Manager/Admin-Facing Endpoints (Menu Engineering)
    // -------------------------------------------------------

    @GetMapping("/matrix")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<MenuEngineeringMatrixDTO>> getMenuMatrix() {
        MenuEngineeringMatrixDTO matrix = recommendationService.getMenuEngineeringMatrix();
        return ResponseEntity.ok(ApiResponse.success("Menu Engineering Matrix generated", matrix));
    }
}
