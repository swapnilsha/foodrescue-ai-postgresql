package com.foodrescue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodrescue.dto.AuthResponse;
import com.foodrescue.dto.DonationRequestDto;
import com.foodrescue.dto.DonationRequestResponse;
import com.foodrescue.dto.FoodDonationRequest;
import com.foodrescue.dto.RegisterRequest;
import com.foodrescue.entity.DonationRequestStatus;
import com.foodrescue.entity.FoodDonation;
import com.foodrescue.entity.Notification;
import com.foodrescue.entity.User;
import com.foodrescue.repository.DonationRequestRepository;
import com.foodrescue.repository.FoodDonationRepository;
import com.foodrescue.repository.NotificationRepository;
import com.foodrescue.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class WorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FoodDonationRepository donationRepository;

    @Autowired
    private DonationRequestRepository requestRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String donorToken;
    private String ngoToken;
    
    private User donorUser;
    private User ngoUser;

    private static final String DONOR_EMAIL = "donor_test_e2e@test.com";
    private static final String NGO_EMAIL = "ngo_test_e2e@test.com";
    private static final String PASSWORD = "password123";

    @BeforeEach
    public void setUp() throws Exception {
        // Clean up any existing requests/notifications to prevent duplicate FK violations
        notificationRepository.deleteAll();
        requestRepository.deleteAll();
        donationRepository.deleteAll();
        
        userRepository.findByEmail(DONOR_EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail(NGO_EMAIL).ifPresent(userRepository::delete);

        // 1. Register Donor
        RegisterRequest registerDonor = new RegisterRequest();
        registerDonor.setName("E2E Test Donor");
        registerDonor.setEmail(DONOR_EMAIL);
        registerDonor.setPassword(PASSWORD);
        registerDonor.setRole("DONOR");
        registerDonor.setPhone("9999999901");
        
        MvcResult donorRegResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDonor)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse donorRegResp = objectMapper.readValue(donorRegResult.getResponse().getContentAsString(), AuthResponse.class);
        donorToken = donorRegResp.getToken();
        donorUser = userRepository.findByEmail(DONOR_EMAIL).orElseThrow();

        // 2. Register NGO
        RegisterRequest registerNgo = new RegisterRequest();
        registerNgo.setName("E2E Test NGO");
        registerNgo.setEmail(NGO_EMAIL);
        registerNgo.setPassword(PASSWORD);
        registerNgo.setRole("NGO");
        registerNgo.setPhone("9999999902");

        MvcResult ngoRegResult = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerNgo)))
                .andExpect(status().isOk())
                .andReturn();
        AuthResponse ngoRegResp = objectMapper.readValue(ngoRegResult.getResponse().getContentAsString(), AuthResponse.class);
        ngoToken = ngoRegResp.getToken();
        ngoUser = userRepository.findByEmail(NGO_EMAIL).orElseThrow();
    }

    @Test
    public void testFullE2EWorkflowAndSecurity() throws Exception {
        // Step 1: Donor uploads food donation
        FoodDonationRequest foodRequest = new FoodDonationRequest();
        foodRequest.setTitle("Vegetable Biryani");
        foodRequest.setFoodCategory("Cooked Food");
        foodRequest.setQuantityKg(new BigDecimal("20.0"));
        foodRequest.setHoursUntilExpiry(24);
        foodRequest.setPickupAddress("E2E Pickup Point, Road 5");
        foodRequest.setNotes("Freshly prepared biryani.");

        MvcResult uploadResult = mockMvc.perform(post("/api/food")
                .header("Authorization", "Bearer " + donorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(foodRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        
        FoodDonation donation = objectMapper.readValue(uploadResult.getResponse().getContentAsString(), FoodDonation.class);
        assertNotNull(donation.getId());
        assertEquals("Vegetable Biryani", donation.getTitle());
        assertEquals("AVAILABLE", donation.getStatus());

        // Step 2: Donor queries available NGOs
        MvcResult ngoListResult = mockMvc.perform(get("/api/ngo/available")
                .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andReturn();
        String listBody = ngoListResult.getResponse().getContentAsString();
        assertTrue(listBody.contains(NGO_EMAIL));

        // Step 3: Donor creates donation request for the NGO
        DonationRequestDto requestDto = new DonationRequestDto();
        requestDto.setDonationId(donation.getId());
        requestDto.setNgoId(ngoUser.getId());

        MvcResult requestResult = mockMvc.perform(post("/api/requests")
                .header("Authorization", "Bearer " + donorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andReturn();

        DonationRequestResponse requestResponse = objectMapper.readValue(requestResult.getResponse().getContentAsString(), DonationRequestResponse.class);
        assertNotNull(requestResponse.getId());
        assertEquals(DonationRequestStatus.PENDING, requestResponse.getStatus());
        assertEquals(donorUser.getName(), requestResponse.getDonorName());
        assertEquals(ngoUser.getName(), requestResponse.getNgoName());

        // Step 4: Verify NGO receives a notification in DB
        List<Notification> ngoNotifs = notificationRepository.findAll().stream()
                .filter(n -> n.getUser().getId().equals(ngoUser.getId()))
                .toList();
        assertFalse(ngoNotifs.isEmpty());
        assertTrue(ngoNotifs.get(0).getMessage().contains("New food donation request received"));

        // Step 5: Duplicate request protection (should fail)
        mockMvc.perform(post("/api/requests")
                .header("Authorization", "Bearer " + donorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().is4xxClientError());

        // Step 6: Security: Donor cannot call Accept API (should return 403 Forbidden)
        mockMvc.perform(patch("/api/requests/" + requestResponse.getId() + "/accept")
                .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isForbidden());

        // Step 7: NGO accepts the request
        MvcResult acceptResult = mockMvc.perform(patch("/api/requests/" + requestResponse.getId() + "/accept")
                .header("Authorization", "Bearer " + ngoToken))
                .andExpect(status().isOk())
                .andReturn();

        DonationRequestResponse acceptedResponse = objectMapper.readValue(acceptResult.getResponse().getContentAsString(), DonationRequestResponse.class);
        assertEquals(DonationRequestStatus.ACCEPTED, acceptedResponse.getStatus());
        assertNotNull(acceptedResponse.getAcceptedAt());

        // Step 7b: Verify donor receives ACCEPTED notification in DB
        List<Notification> donorNotifs = notificationRepository.findAll().stream()
                .filter(n -> n.getUser().getId().equals(donorUser.getId()))
                .toList();
        assertFalse(donorNotifs.isEmpty());
        assertTrue(donorNotifs.stream().anyMatch(n -> n.getMessage().toLowerCase().contains("accepted")));

        // Step 8: NGO assigns delivery
        Map<String, String> assignBody = new HashMap<>();
        assignBody.put("assignedToName", "Test Delivery Person");
        assignBody.put("assignedToPhone", "9999999999");

        MvcResult assignResult = mockMvc.perform(patch("/api/requests/" + requestResponse.getId() + "/assign")
                .header("Authorization", "Bearer " + ngoToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(assignBody)))
                .andExpect(status().isOk())
                .andReturn();

        DonationRequestResponse assignedResponse = objectMapper.readValue(assignResult.getResponse().getContentAsString(), DonationRequestResponse.class);
        assertEquals(DonationRequestStatus.ASSIGNED, assignedResponse.getStatus());

        // Step 9: NGO starts pickup
        MvcResult pickupResult = mockMvc.perform(patch("/api/requests/" + requestResponse.getId() + "/pickup-start")
                .header("Authorization", "Bearer " + ngoToken))
                .andExpect(status().isOk())
                .andReturn();
        
        DonationRequestResponse pickupResponse = objectMapper.readValue(pickupResult.getResponse().getContentAsString(), DonationRequestResponse.class);
        assertEquals(DonationRequestStatus.PICKUP_IN_PROGRESS, pickupResponse.getStatus());
        assertNotNull(pickupResponse.getPickupStartedAt());

        // Step 10: NGO collects food
        MvcResult collectResult = mockMvc.perform(patch("/api/requests/" + requestResponse.getId() + "/collect")
                .header("Authorization", "Bearer " + ngoToken))
                .andExpect(status().isOk())
                .andReturn();

        DonationRequestResponse collectResponse = objectMapper.readValue(collectResult.getResponse().getContentAsString(), DonationRequestResponse.class);
        assertEquals(DonationRequestStatus.COLLECTED, collectResponse.getStatus());
        assertNotNull(collectResponse.getCollectedAt());

        // Step 11: NGO confirms receive
        Map<String, Object> receiveBody = new HashMap<>();
        receiveBody.put("receivedQuantityKg", 19.0);
        receiveBody.put("notes", "Received safely. 19 KG food received.");

        MvcResult receiveResult = mockMvc.perform(patch("/api/requests/" + requestResponse.getId() + "/receive")
                .header("Authorization", "Bearer " + ngoToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(receiveBody)))
                .andExpect(status().isOk())
                .andReturn();

        DonationRequestResponse receiveResponse = objectMapper.readValue(receiveResult.getResponse().getContentAsString(), DonationRequestResponse.class);
        assertEquals(DonationRequestStatus.RECEIVED, receiveResponse.getStatus());
        assertEquals(0, new BigDecimal("19.0").compareTo(receiveResponse.getReceivedQuantityKg()));

        // Step 12: NGO delivers food
        Map<String, String> deliverBody = new HashMap<>();
        deliverBody.put("notes", "Delivered to shelter.");

        MvcResult deliverResult = mockMvc.perform(patch("/api/requests/" + requestResponse.getId() + "/deliver")
                .header("Authorization", "Bearer " + ngoToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deliverBody)))
                .andExpect(status().isOk())
                .andReturn();

        DonationRequestResponse deliverResponse = objectMapper.readValue(deliverResult.getResponse().getContentAsString(), DonationRequestResponse.class);
        assertEquals(DonationRequestStatus.DELIVERED, deliverResponse.getStatus());
        assertNotNull(deliverResponse.getDeliveredAt());

        // Step 13: Invalid status transition (going backward from DELIVERED to ACCEPTED must fail)
        mockMvc.perform(patch("/api/requests/" + requestResponse.getId() + "/accept")
                .header("Authorization", "Bearer " + ngoToken))
                .andExpect(status().is4xxClientError());
    }
}
