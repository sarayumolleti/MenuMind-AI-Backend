package com.menumind.backend.dto.feedback;

import com.menumind.backend.entity.Feedback;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private Long id;
    private String customerName;
    private Long menuItemId;
    private String menuItemName;
    private Integer rating;
    private String reviewText;
    private String sentiment;
    private LocalDateTime createdAt;

    public static FeedbackResponse fromEntity(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .customerName(feedback.getCustomerName() != null ? feedback.getCustomerName() : (feedback.getCustomer() != null ? feedback.getCustomer().getFullName() : "Guest"))
                .menuItemId(feedback.getMenuItem() != null ? feedback.getMenuItem().getId() : null)
                .menuItemName(feedback.getMenuItem() != null ? feedback.getMenuItem().getName() : "General Experience")
                .rating(feedback.getRating())
                .reviewText(feedback.getReviewText())
                .sentiment(feedback.getSentiment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
