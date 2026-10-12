package com.menumind.backend.controller;

import com.menumind.backend.dto.common.ApiResponse;
import com.menumind.backend.dto.feedback.FeedbackAnalyticsDTO;
import com.menumind.backend.dto.feedback.FeedbackRequest;
import com.menumind.backend.dto.feedback.FeedbackResponse;
import com.menumind.backend.entity.User;
import com.menumind.backend.repository.UserRepository;
import com.menumind.backend.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> submitFeedback(
            @Valid @RequestBody FeedbackRequest request,
            Authentication authentication) {

        User currentUser = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            currentUser = userRepository.findByEmail(authentication.getName()).orElse(null);
        }

        FeedbackResponse response = feedbackService.submitFeedback(request, currentUser);
        return new ResponseEntity<>(ApiResponse.success("Thank you for your feedback!", response), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getAllFeedbacks() {
        List<FeedbackResponse> list = feedbackService.getAllFeedbacks();
        return ResponseEntity.ok(ApiResponse.success("Customer feedbacks retrieved successfully", list));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<FeedbackAnalyticsDTO>> getFeedbackAnalytics() {
        FeedbackAnalyticsDTO analytics = feedbackService.getFeedbackAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Feedback analytics generated successfully", analytics));
    }
}
