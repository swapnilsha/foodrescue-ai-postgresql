package com.foodrescue.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AIAnalysisResult {
    private double confidenceScore;
    private double freshnessScore;
    private double estimatedQuantityKg;
    private String predictedExpiryTime; // ISO string for JSON serialization
    private String recommendation;
    private String freshnessLabel; // FRESH, ACCEPTABLE, POOR
}