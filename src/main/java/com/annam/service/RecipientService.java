package com.annam.service;

import com.annam.model.Recipient;
import com.annam.repository.RecipientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecipientService {
    private final RecipientRepository recipientRepository;

    public RecipientService(RecipientRepository recipientRepository) {
        this.recipientRepository = recipientRepository;
    }

    public List<Recipient> getAllRecipients() {
        return recipientRepository.findAll();
    }

    public Recipient getRecipientById(String recipientId) {
        return recipientRepository.findById(recipientId);
    }

    public void saveRecipient(Recipient recipient) {
        recipientRepository.save(recipient);
    }

    public void updateDemand(String recipientId, int demand) {
        recipientRepository.updateDemand(recipientId, demand);
    }
}
