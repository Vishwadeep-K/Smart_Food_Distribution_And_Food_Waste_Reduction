package com.annam.service;

import com.annam.model.DeliveryVehicle;
import com.annam.repository.VehicleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleService {
    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public List<DeliveryVehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public DeliveryVehicle getVehicleById(String vehicleId) {
        return vehicleRepository.findById(vehicleId);
    }

    public void saveVehicle(DeliveryVehicle vehicle) {
        vehicleRepository.save(vehicle);
    }

    public void setAvailable(String vehicleId, boolean available) {
        vehicleRepository.setAvailable(vehicleId, available);
    }

    public void updateLocationAndAvailability(String vehicleId, String locationId, boolean available) {
        vehicleRepository.updateLocationAndAvailability(vehicleId, locationId, available);
    }
}
