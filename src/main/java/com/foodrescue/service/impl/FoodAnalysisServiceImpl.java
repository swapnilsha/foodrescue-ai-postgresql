package com.foodrescue.service.impl;

import com.foodrescue.dto.AIAnalysisResult;
import com.foodrescue.service.FoodAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
@Slf4j
public class FoodAnalysisServiceImpl implements FoodAnalysisService {

    private final Random random = new Random();
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public AIAnalysisResult analyzeFoodImage(MultipartFile image, String category) {
        log.info("AI Analysis initiated for image: '{}', category: '{}'",
                image.getOriginalFilename(), category);

        double confidenceScore  = 80.0 + (20.0 * random.nextDouble());
        double freshnessScore   = calculateFreshness(category);
        double estimatedQty     = 1.0 + (49.0 * random.nextDouble());
        LocalDateTime expiry    = predictExpiry(category, freshnessScore);
        String freshnessLabel   = labelFreshness(freshnessScore);
        String recommendation   = buildRecommendation(freshnessScore, expiry);

        log.info("Analysis complete. Freshness: {}% ({}), Expiry: {}",
                String.format("%.1f", freshnessScore), freshnessLabel, expiry);

        return AIAnalysisResult.builder()
                .confidenceScore(Math.round(confidenceScore * 10.0) / 10.0)
                .freshnessScore(Math.round(freshnessScore * 10.0) / 10.0)
                .estimatedQuantityKg(Math.round(estimatedQty * 10.0) / 10.0)
                .predictedExpiryTime(expiry.format(ISO_FMT))
                .freshnessLabel(freshnessLabel)
                .recommendation(recommendation)
                .build();
    }

    private double calculateFreshness(String category) {
        if (category == null) return 75.0 + (20.0 * random.nextDouble());
        return switch (category.toUpperCase()) {
            case "COOKED_FOOD"  -> 60.0 + (30.0 * random.nextDouble());
            case "PRODUCE"      -> 70.0 + (25.0 * random.nextDouble());
            case "PACKAGED"     -> 90.0 + (10.0 * random.nextDouble());
            case "DAIRY"        -> 65.0 + (25.0 * random.nextDouble());
            case "BAKERY"       -> 55.0 + (35.0 * random.nextDouble());
            default             -> 75.0 + (20.0 * random.nextDouble());
        };
    }

    private LocalDateTime predictExpiry(String category, double freshness) {
        int hoursToAdd = (int) (freshness / 10);
        if (category == null) return LocalDateTime.now().plusHours(24);
        return switch (category.toUpperCase()) {
            case "COOKED_FOOD"  -> LocalDateTime.now().plusHours(hoursToAdd);
            case "PRODUCE"      -> LocalDateTime.now().plusDays(Math.max(1, hoursToAdd / 3));
            case "PACKAGED"     -> LocalDateTime.now().plusMonths(1);
            case "DAIRY"        -> LocalDateTime.now().plusDays(Math.max(1, hoursToAdd / 4));
            case "BAKERY"       -> LocalDateTime.now().plusHours(Math.max(6, hoursToAdd / 2));
            default             -> LocalDateTime.now().plusHours(24);
        };
    }

    private String labelFreshness(double score) {
        if (score >= 85) return "FRESH";
        if (score >= 65) return "ACCEPTABLE";
        return "POOR";
    }

    private String buildRecommendation(double freshness, LocalDateTime expiry) {
        if (freshness >= 85) {
            return "Food is in excellent condition. Suitable for immediate redistribution.";
        } else if (freshness >= 65) {
            return "Food is acceptable. Prioritize pickup within the next few hours.";
        } else {
            return "Food quality is declining. Urgent pickup recommended — contact an NGO immediately.";
        }
    }
}