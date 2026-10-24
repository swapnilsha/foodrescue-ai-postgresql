package com.foodrescue.service;

import com.foodrescue.dto.AIAnalysisResult;
import org.springframework.web.multipart.MultipartFile;

public interface FoodAnalysisService {
    AIAnalysisResult analyzeFoodImage(MultipartFile image, String category);
}