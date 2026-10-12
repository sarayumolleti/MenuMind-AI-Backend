package com.menumind.backend.service.impl;

import com.menumind.backend.dto.feedback.FeedbackAnalyticsDTO;
import com.menumind.backend.dto.feedback.FeedbackRequest;
import com.menumind.backend.dto.feedback.FeedbackResponse;
import com.menumind.backend.entity.Feedback;
import com.menumind.backend.entity.MenuItem;
import com.menumind.backend.entity.User;
import com.menumind.backend.repository.FeedbackRepository;
import com.menumind.backend.repository.MenuItemRepository;
import com.menumind.backend.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final MenuItemRepository menuItemRepository;

    @Override
    @Transactional
    public FeedbackResponse submitFeedback(FeedbackRequest request, User currentUser) {
        MenuItem menuItem = null;
        if (request.getMenuItemId() != null) {
            menuItem = menuItemRepository.findById(request.getMenuItemId()).orElse(null);
        }

        String sentiment = analyzeSentiment(request.getRating(), request.getReviewText());

        String name = request.getCustomerName();
        if ((name == null || name.trim().isEmpty()) && currentUser != null) {
            name = currentUser.getFullName();
        }
        if (name == null || name.trim().isEmpty()) {
            name = "Guest Diner";
        }

        Feedback feedback = Feedback.builder()
                .customer(currentUser)
                .customerName(name.trim())
                .menuItem(menuItem)
                .rating(request.getRating())
                .reviewText(request.getReviewText() != null ? request.getReviewText().trim() : "")
                .sentiment(sentiment)
                .build();

        Feedback saved = feedbackRepository.save(feedback);
        log.info("Saved customer feedback id: {}, rating: {}, sentiment: {}", saved.getId(), saved.getRating(), saved.getSentiment());
        return FeedbackResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getAllFeedbacks() {
        return feedbackRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(FeedbackResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackAnalyticsDTO getFeedbackAnalytics() {
        List<Feedback> all = feedbackRepository.findAllByOrderByCreatedAtDesc();
        if (all.isEmpty()) {
            return FeedbackAnalyticsDTO.builder()
                    .averageRating(0.0)
                    .totalReviews(0L)
                    .positiveCount(0L)
                    .neutralCount(0L)
                    .negativeCount(0L)
                    .mostLikedDishes(Collections.emptyList())
                    .commonComplaints(Collections.emptyList())
                    .improvementSuggestions(Collections.emptyList())
                    .recentFeedbacks(Collections.emptyList())
                    .build();
        }

        double avgRating = all.stream().mapToInt(Feedback::getRating).average().orElse(0.0);
        long positiveCount = all.stream().filter(f -> "POSITIVE".equalsIgnoreCase(f.getSentiment())).count();
        long neutralCount = all.stream().filter(f -> "NEUTRAL".equalsIgnoreCase(f.getSentiment())).count();
        long negativeCount = all.stream().filter(f -> "NEGATIVE".equalsIgnoreCase(f.getSentiment())).count();

        // Dish ratings
        Map<String, List<Integer>> dishRatings = new HashMap<>();
        List<String> complaints = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        for (Feedback f : all) {
            if (f.getMenuItem() != null) {
                dishRatings.computeIfAbsent(f.getMenuItem().getName(), k -> new ArrayList<>()).add(f.getRating());
            }

            String text = f.getReviewText() != null ? f.getReviewText().toLowerCase() : "";
            if (!text.isEmpty()) {
                if (text.contains("cold") || text.contains("slow") || text.contains("delay") || text.contains("late") || text.contains("waiting")) {
                    complaints.add("Order delivery / service time delays reported");
                }
                if (text.contains("salty") || text.contains("spice") || text.contains("spicy") || text.contains("bland") || text.contains("oil")) {
                    complaints.add("Food taste / seasoning consistency needs attention");
                }
                if (text.contains("price") || text.contains("expensive") || text.contains("cost") || text.contains("portion")) {
                    complaints.add("Portion size or pricing adjustments suggested");
                }
                if (text.contains("sweet") || text.contains("sugar") || text.contains("dessert")) {
                    complaints.add("Beverage or sweetness level feedback noted");
                }
            }
        }

        // Most liked dishes (avg rating >= 4.0 and count >= 1)
        List<String> mostLiked = dishRatings.entrySet().stream()
                .filter(e -> e.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0) >= 4.0)
                .sorted((a, b) -> Double.compare(
                        b.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0),
                        a.getValue().stream().mapToInt(Integer::intValue).average().orElse(0.0)
                ))
                .map(Map.Entry::getKey)
                .limit(5)
                .collect(Collectors.toList());

        // Deduplicate complaints
        List<String> uniqueComplaints = complaints.stream().distinct().limit(5).collect(Collectors.toList());
        if (uniqueComplaints.isEmpty() && negativeCount > 0) {
            uniqueComplaints.add("General lower rating feedback on service or food taste");
        }

        // Suggestions based on complaints and sentiment
        if (negativeCount > 0) {
            suggestions.add("Audit preparation times for peak rush hours to improve serving speed");
            suggestions.add("Ensure head chef monitors spice and salt consistency during bulk prep");
        }
        if (avgRating < 4.0 && all.size() >= 3) {
            suggestions.add("Introduce staff customer-service refresher and follow up with dissatisfied diners");
        }
        if (positiveCount > 0) {
            suggestions.add("Highlight top-rated customer favorite dishes as Chef Recommendations on the menu");
        }

        List<FeedbackResponse> recents = all.stream().limit(10).map(FeedbackResponse::fromEntity).collect(Collectors.toList());

        return FeedbackAnalyticsDTO.builder()
                .averageRating(Math.round(avgRating * 10.0) / 10.0)
                .totalReviews((long) all.size())
                .positiveCount(positiveCount)
                .neutralCount(neutralCount)
                .negativeCount(negativeCount)
                .mostLikedDishes(mostLiked)
                .commonComplaints(uniqueComplaints)
                .improvementSuggestions(suggestions)
                .recentFeedbacks(recents)
                .build();
    }

    private String analyzeSentiment(int rating, String text) {
        String lower = text != null ? text.toLowerCase() : "";
        int positiveWords = 0;
        int negativeWords = 0;

        String[] posKeywords = {"good", "great", "excellent", "amazing", "delicious", "tasty", "love", "loved", "fresh", "best", "superb", "fast", "friendly"};
        String[] negKeywords = {"bad", "poor", "terrible", "horrible", "cold", "slow", "delay", "bland", "salty", "oily", "worst", "hate", "stale"};

        for (String w : posKeywords) {
            if (lower.contains(w)) positiveWords++;
        }
        for (String w : negKeywords) {
            if (lower.contains(w)) negativeWords++;
        }

        if (rating >= 4) {
            return negativeWords > positiveWords && rating == 4 ? "NEUTRAL" : "POSITIVE";
        } else if (rating <= 2) {
            return positiveWords > negativeWords && rating == 2 ? "NEUTRAL" : "NEGATIVE";
        } else {
            // Rating 3
            if (positiveWords > negativeWords) return "POSITIVE";
            if (negativeWords > positiveWords) return "NEGATIVE";
            return "NEUTRAL";
        }
    }
}
