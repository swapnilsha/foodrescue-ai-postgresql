package com.foodrescue.controller;

import com.foodrescue.dto.AIAnalysisResult;
import com.foodrescue.service.FoodAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Analysis", description = "Food freshness analysis using AI")
@SecurityRequirement(name = "Bearer Authentication")
public class AIController {

    private final FoodAnalysisService foodAnalysisService;

    /**
     * B-13 FIX: POST /api/ai/analyze — AI food analysis endpoint.
     * Called by ai-workflow.js when user uploads food image.
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('DONOR', 'ADMIN', 'NGO')")
    @Operation(summary = "Analyze a food image for freshness and expiry prediction")
    public ResponseEntity<AIAnalysisResult> analyzeFood(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "category", defaultValue = "OTHER") String category) {

        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        AIAnalysisResult result = foodAnalysisService.analyzeFoodImage(image, category);
        return ResponseEntity.ok(result);
    }
}
