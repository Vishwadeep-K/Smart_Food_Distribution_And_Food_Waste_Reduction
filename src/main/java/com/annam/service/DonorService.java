package com.annam.service;

import com.annam.model.Donor;
import com.annam.repository.DonorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DonorService {
    private final DonorRepository donorRepository;

    public DonorService(DonorRepository donorRepository) {
        this.donorRepository = donorRepository;
    }

    public List<Donor> getAllDonors() {
        return donorRepository.findAll();
    }

    public Donor getDonorById(String donorId) {
        return donorRepository.findById(donorId);
    }

    public void saveDonor(Donor donor) {
        donorRepository.save(donor);
    }

    public void deleteDonor(String donorId) {
        donorRepository.delete(donorId);
    }

    public int getDonationBatchCount(String donorId) {
        return donorRepository.countBatchesByDonor(donorId);
    }
}
