package com.foodrescue.mapper;

import com.foodrescue.dto.FoodDonationRequest;
import com.foodrescue.entity.FoodDonation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring", imports = {LocalDateTime.class})
public interface FoodDonationMapper {

    @Mappings({
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "donor", ignore = true),
        @Mapping(target = "location", ignore = true),
        @Mapping(target = "status", constant = "AVAILABLE"),
        @Mapping(target = "createdAt", ignore = true),
        @Mapping(target = "imagePath", ignore = true),
        @Mapping(target = "acceptedByNgo", ignore = true),
        // Map optional fields from request DTO
        @Mapping(target = "urgency", source = "urgency"),
        @Mapping(target = "notes", source = "notes"),
        // Calculate exact expiry time based on the integer provided in the request
        @Mapping(target = "expiryTime", expression = "java(LocalDateTime.now().plusHours(request.getHoursUntilExpiry()))")
    })
    FoodDonation toEntity(FoodDonationRequest request);
}