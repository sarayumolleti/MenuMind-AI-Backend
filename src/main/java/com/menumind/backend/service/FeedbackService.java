package com.menumind.backend.service;

import com.menumind.backend.dto.feedback.FeedbackAnalyticsDTO;
import com.menumind.backend.dto.feedback.FeedbackRequest;
import com.menumind.backend.dto.feedback.FeedbackResponse;
import com.menumind.backend.entity.User;

import java.util.List;

public interface FeedbackService {
    FeedbackResponse submitFeedback(FeedbackRequest request, User currentUser);
    List<FeedbackResponse> getAllFeedbacks();
    FeedbackAnalyticsDTO getFeedbackAnalytics();
}
