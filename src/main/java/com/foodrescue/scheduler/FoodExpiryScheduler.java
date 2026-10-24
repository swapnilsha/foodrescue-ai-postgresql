package com.foodrescue.scheduler;

import com.foodrescue.entity.FoodDonation;
import com.foodrescue.repository.FoodDonationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class FoodExpiryScheduler {

    private final FoodDonationRepository donationRepository;
    // private final WebSocketNotificationService notificationService;

    @Scheduled(cron = "0 0 * * * *") // Runs at the top of every hour
    @Transactional
    public void checkFoodExpiries() {
        log.info("Running Food Expiry Scheduler at {}", LocalDateTime.now());
        
        List<FoodDonation> availableFoods = donationRepository.findByStatus("AVAILABLE");
        int expiredCount = 0;

        for (FoodDonation food : availableFoods) {
            if (food.getExpiryTime().isBefore(LocalDateTime.now())) {
                food.setStatus("EXPIRED");
                donationRepository.save(food);
                expiredCount++;
                
                // notificationService.notifyDonor(food.getDonor().getId(), "Your donation '" + food.getTitle() + "' has expired.");
            }
        }
        
        log.info("Scheduler completed. Marked {} items as expired.", expiredCount);
    }
}