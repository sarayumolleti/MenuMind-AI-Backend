package com.menumind.backend.dto.feedback;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackAnalyticsDTO {
    private Double averageRating;
    private Long totalReviews;
    private Long positiveCount;
    private Long neutralCount;
    private Long negativeCount;
    private List<String> mostLikedDishes;
    private List<String> commonComplaints;
    private List<String> improvementSuggestions;
    private List<FeedbackResponse> recentFeedbacks;
}
