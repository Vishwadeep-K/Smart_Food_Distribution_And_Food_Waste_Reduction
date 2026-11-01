package com.annam.service;

import com.annam.dto.DonorDonationDto;
import com.annam.dto.DonorProfileDto;
import com.annam.dto.SubmitDonationRequest;
import com.annam.graph.Location;
import com.annam.model.*;
import com.annam.repository.DonorRepository;
import com.annam.repository.FoodBatchRepository;
import com.annam.repository.LocationRepository;
import com.annam.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class DonorPortalService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserRepository userRepository;
    private final DonorRepository donorRepository;
    private final FoodBatchRepository foodBatchRepository;
    private final FoodBatchService foodBatchService;
    private final LocationRepository locationRepository;
    private final com.annam.repository.AllocationRepository allocationRepository;

    public DonorPortalService(UserRepository userRepository, DonorRepository donorRepository,
                              FoodBatchRepository foodBatchRepository, FoodBatchService foodBatchService,
                              LocationRepository locationRepository,
                              com.annam.repository.AllocationRepository allocationRepository) {
        this.userRepository = userRepository;
        this.donorRepository = donorRepository;
        this.foodBatchRepository = foodBatchRepository;
        this.foodBatchService = foodBatchService;
        this.locationRepository = locationRepository;
        this.allocationRepository = allocationRepository;
    }

    public DonorProfileDto getDonorProfile(User user) {
        if (user == null || user.getDonorId() == null) {
            throw new IllegalArgumentException("User is not associated with a Donor profile.");
        }

        Donor donor = donorRepository.findById(user.getDonorId());
        if (donor == null) {
            throw new IllegalArgumentException("Donor record not found for ID: " + user.getDonorId());
        }

        List<FoodBatch> donations = foodBatchRepository.findByDonorId(donor.getDonorId());
        int totalPortions = donations.stream().mapToInt(FoodBatch::getTotalPortions).sum();
        int deliveredPortions = donations.stream().mapToInt(FoodBatch::getDeliveredPortions).sum();
        
        int pendingCount = (int) donations.stream()
                .filter(b -> b.getApprovalStatus() == ApprovalStatus.NOT_APPROVED && !b.isExpired())
                .count();

        int deliveredDonationsCount = (int) donations.stream()
                .filter(b -> b.getDeliveredPortions() >= b.getTotalPortions() && b.getTotalPortions() > 0)
                .count();

        int expiredOrRejectedCount = (int) donations.stream()
                .filter(b -> b.isExpired() || b.getApprovalStatus() == ApprovalStatus.REJECTED)
                .count();

        DonorProfileDto dto = new DonorProfileDto();
        dto.setDonorId(donor.getDonorId());
        dto.setName(donor.getName());
        dto.setOrganization(donor.getOrganization());
        dto.setDonorType(donor.getDonorType());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : donor.getPhone());
        dto.setCity(donor.getCity() != null ? donor.getCity() : "");
        dto.setLocationId(donor.getLocation().getLocationId());
        dto.setLocationName(donor.getLocation().getName());
        dto.setPickupAddress(user.getPickupAddress() != null && !user.getPickupAddress().isEmpty() 
                ? user.getPickupAddress() : donor.getLocation().getName());

        // 5 Summary metrics matching user requirements
        dto.setTotalDonationsCount(donations.size());
        dto.setTotalPortionsDonated(totalPortions);
        dto.setPendingApprovalCount(pendingCount);
        dto.setDeliveredDonationsCount(deliveredDonationsCount);
        dto.setDeliveredPortionsCount(deliveredPortions);
        dto.setExpiredOrRejectedCount(expiredOrRejectedCount);

        return dto;
    }

    @Transactional
    public DonorProfileDto updateDonorProfile(User user, String name, String phone, String pickupAddress, String locationId) {
        if (user == null || user.getDonorId() == null) {
            throw new IllegalArgumentException("User is not associated with a Donor profile.");
        }

        Donor donor = donorRepository.findById(user.getDonorId());
        if (donor == null) {
            throw new IllegalArgumentException("Donor record not found for ID: " + user.getDonorId());
        }

        if (name != null && !name.trim().isEmpty()) {
            donor.setName(name.trim());
            donor.setOrganization(name.trim());
            user.setFullName(name.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            donor.setPhone(phone.trim());
            user.setPhone(phone.trim());
        }
        if (pickupAddress != null && !pickupAddress.trim().isEmpty()) {
            user.setPickupAddress(pickupAddress.trim());
        }
        if (locationId != null && !locationId.trim().isEmpty()) {
            Location loc = locationRepository.findById(locationId.trim().toUpperCase());
            if (loc != null) {
                donor.setLocation(loc);
            }
        }

        donorRepository.save(donor);
        userRepository.updateProfile(user.getUserId(), user.getFullName(), user.getPhone(), user.getPickupAddress());

        return getDonorProfile(user);
    }

    @Transactional
    public DonorDonationDto submitDonation(User user, SubmitDonationRequest request) {
        if (user == null || user.getDonorId() == null) {
            throw new IllegalArgumentException("Unauthorized: Only registered donors can submit donations.");
        }

        Donor donor = donorRepository.findById(user.getDonorId());
        if (donor == null) {
            throw new IllegalArgumentException("Donor profile not found.");
        }

        if (request == null) {
            throw new IllegalArgumentException("Donation request cannot be null.");
        }

        String catStr = request.getCategory();
        if (catStr == null || catStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Food category is required.");
        }

        FoodCategory category;
        try {
            category = FoodCategory.valueOf(catStr.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid food category: " + catStr);
        }

        if (request.getTotalPortions() == null || request.getTotalPortions() <= 0) {
            throw new IllegalArgumentException("Available quantity/portions must be greater than zero.");
        }

        String useByStr = request.getUseByTime();
        if (useByStr == null || useByStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Use-by time is required.");
        }
        if (useByStr.length() == 5) useByStr += ":00";

        LocalTime useByTime;
        try {
            useByTime = LocalTime.parse(useByStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid use-by time format (HH:mm required): " + useByStr);
        }

        LocalTime prepTime = null;
        if (request.getPreparationTime() != null && !request.getPreparationTime().trim().isEmpty()) {
            String prepStr = request.getPreparationTime().trim();
            if (prepStr.length() == 5) prepStr += ":00";
            try {
                prepTime = LocalTime.parse(prepStr);
            } catch (Exception ignored) {}
        }

        Location pickupLoc = donor.getLocation();
        if (request.getPickupLocationId() != null && !request.getPickupLocationId().trim().isEmpty()) {
            Location reqLoc = locationRepository.findById(request.getPickupLocationId().trim().toUpperCase());
            if (reqLoc != null) {
                pickupLoc = reqLoc;
            }
        }

        String foodName = request.getFoodName() != null && !request.getFoodName().trim().isEmpty()
                ? request.getFoodName().trim() : category.getDisplayName();

        String batchId = foodBatchRepository.getNextBatchId();

        FoodBatch batch = new FoodBatch(
                batchId,
                category,
                foodName,
                request.getTotalPortions(),
                request.getTotalPortions(), // initially available
                0,                          // reserved
                0,                          // delivered
                prepTime,
                useByTime,
                donor,
                pickupLoc,
                request.getNotes() != null ? request.getNotes().trim() : null,
                ApprovalStatus.NOT_APPROVED, // Initial status: Pending Approval
                false,
                LocalDateTime.now()
        );

        foodBatchService.saveBatch(batch);

        return toDonationDto(batch);
    }

    public List<DonorDonationDto> getDonationsForDonor(User user) {
        if (user == null || user.getDonorId() == null) {
            return Collections.emptyList();
        }

        List<FoodBatch> list = foodBatchRepository.findByDonorId(user.getDonorId());
        return list.stream().map(this::toDonationDto).toList();
    }

    public DonorDonationDto getDonationById(User user, String batchId) {
        if (user == null || user.getDonorId() == null || batchId == null) {
            return null;
        }

        FoodBatch batch = foodBatchRepository.findById(batchId.trim().toUpperCase());
        if (batch == null) return null;

        // Security check: Only allow donor to access their own donation
        if (!batch.getDonor().getDonorId().equalsIgnoreCase(user.getDonorId())) {
            throw new IllegalArgumentException("Access Denied: You can only view your own donations.");
        }

        return toDonationDto(batch);
    }

    public DonorDonationDto toDonationDto(FoodBatch b) {
        DonorDonationDto dto = new DonorDonationDto();
        dto.setBatchId(b.getBatchId());
        dto.setFoodName(b.getFoodName());
        dto.setCategory(b.getCategory().name());
        dto.setCategoryDisplayName(b.getCategory().getDisplayName());
        dto.setTotalPortions(b.getTotalPortions());
        dto.setAvailablePortions(b.getAvailablePortions());
        dto.setReservedPortions(b.getReservedPortions());
        dto.setDeliveredPortions(b.getDeliveredPortions());
        dto.setRemainingPortions(b.getAvailablePortions());
        dto.setPreparationTime(b.getPreparationTime() != null ? b.getPreparationTime().format(TIME_FORMAT) : "--");
        dto.setDeclaredUseByTime(b.getDeclaredUseByTime().format(TIME_FORMAT));
        dto.setPickupLocationId(b.getPickupLocation().getLocationId());
        dto.setPickupLocationName(b.getPickupLocation().getName());
        dto.setNotes(b.getNotes() != null ? b.getNotes() : "");
        dto.setRejectionReason(b.getRejectionReason() != null ? b.getRejectionReason() : "");
        dto.setApprovalStatus(b.getApprovalStatus().name());
        dto.setExpired(b.isExpired());

        // Human-friendly display approval status
        switch (b.getApprovalStatus()) {
            case APPROVED -> dto.setDisplayApprovalStatus("Approved");
            case REJECTED -> dto.setDisplayApprovalStatus("Rejected");
            case NOT_APPROVED -> dto.setDisplayApprovalStatus("Pending Approval");
        }

        // Detailed allocation and delivery status calculation:
        // Indicators: Pending Approval, Approved, Partially Allocated, Reserved, Delivered, Rejected, Expired
        if (b.isExpired()) {
            dto.setDeliveryStatus("Expired");
        } else if (b.getApprovalStatus() == ApprovalStatus.REJECTED) {
            dto.setDeliveryStatus("Rejected");
        } else if (b.getApprovalStatus() == ApprovalStatus.NOT_APPROVED) {
            dto.setDeliveryStatus("Pending Approval");
        } else if (b.getDeliveredPortions() >= b.getTotalPortions()) {
            dto.setDeliveryStatus("Delivered");
        } else if ((b.getDeliveredPortions() > 0 && b.getDeliveredPortions() < b.getTotalPortions())
                || (b.getReservedPortions() > 0 && b.getAvailablePortions() > 0)) {
            dto.setDeliveryStatus("Partially Allocated");
        } else if (b.getReservedPortions() > 0 && b.getAvailablePortions() == 0) {
            dto.setDeliveryStatus("Reserved");
        } else {
            dto.setDeliveryStatus("Approved");
        }

        dto.setCreatedAt(b.getCreatedAt() != null ? b.getCreatedAt().format(DATETIME_FORMAT) : "");

        // Fetch allocations linked to this batch for recipient and delivery disclosure
        try {
            List<FoodAllocation> allocations = allocationRepository.findByBatchId(b.getBatchId());
            if (allocations != null && !allocations.isEmpty()) {
                List<DonorDonationDto.AllocationDetailDto> allocDtos = new ArrayList<>();
                for (FoodAllocation a : allocations) {
                    allocDtos.add(new DonorDonationDto.AllocationDetailDto(
                            a.getAllocationId(),
                            a.getRecipient().getName(),
                            a.getRecipient().getRecipientType(),
                            a.getVehicle().getName(),
                            a.getAllocatedPortions(),
                            a.getPickupLocation().getName(),
                            a.getDestinationLocation().getName(),
                            a.getPickupTime() != null ? a.getPickupTime().toString().substring(0, 5) : "--",
                            a.getExpectedCompletionTime() != null ? a.getExpectedCompletionTime().toString().substring(0, 5) : "--",
                            a.getStatus().name()
                    ));
                }
                dto.setAllocations(allocDtos);
            }
        } catch (Exception ignored) {}

        return dto;
    }
}
