package com.annam.service;

import com.annam.config.DatabaseInitConfig;
import com.annam.exception.InvalidAllocationException;
import com.annam.model.ApprovalStatus;
import com.annam.model.FoodBatch;
import com.annam.repository.FoodBatchRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FoodBatchService {
    private final FoodBatchRepository batchRepository;
    private final Map<String, FoodBatch> batchLookupMap;
    private final PriorityQueue<FoodBatch> approvedPriorityQueue;

    // DatabaseInitConfig dependency ensures DB migrations (column additions) complete before this service queries
    public FoodBatchService(FoodBatchRepository batchRepository, DatabaseInitConfig dbInit) {
        this.batchRepository = batchRepository;
        this.batchLookupMap = new HashMap<>();
        this.approvedPriorityQueue = new PriorityQueue<>();
        reloadFromDatabase();
    }

    public synchronized void reloadFromDatabase() {
        batchLookupMap.clear();
        approvedPriorityQueue.clear();

        List<FoodBatch> all = batchRepository.findAll();
        for (FoodBatch b : all) {
            batchLookupMap.put(b.getBatchId().toUpperCase(), b);
            if (b.isApproved() && !b.isExpired() && b.getAvailablePortions() > 0) {
                approvedPriorityQueue.add(b);
            }
        }
    }

    public synchronized List<FoodBatch> getAllBatches() {
        return new ArrayList<>(batchLookupMap.values());
    }

    public synchronized FoodBatch getBatchById(String batchId) {
        if (batchId == null) return null;
        return batchLookupMap.get(batchId.toUpperCase());
    }

    public synchronized List<FoodBatch> getApprovedPriorityQueueOrder() {
        // Return a copy sorted according to PriorityQueue ordering (Earliest use-by time first)
        List<FoodBatch> list = new ArrayList<>(approvedPriorityQueue);
        Collections.sort(list);
        return list;
    }

    public synchronized String getNextBatchId() {
        return batchRepository.getNextBatchId();
    }

    public synchronized void saveBatch(FoodBatch batch) {
        if (batch == null) return;
        // PriorityQueue synchronization rule: remove first
        approvedPriorityQueue.remove(batch);

        batchRepository.save(batch);
        batchLookupMap.put(batch.getBatchId().toUpperCase(), batch);

        if (batch.isApproved() && !batch.isExpired() && batch.getAvailablePortions() > 0) {
            approvedPriorityQueue.add(batch);
        }
    }

    public synchronized boolean approveBatch(String batchId) {
        FoodBatch batch = getBatchById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("Food batch " + batchId + " not found.");
        }
        if (batch.isExpired()) {
            throw new InvalidAllocationException("Cannot approve batch " + batchId + " because it is EXPIRED.");
        }

        // 1. Remove from PriorityQueue if present
        approvedPriorityQueue.remove(batch);

        // 2. Update ranking / approval fields
        batch.setApprovalStatus(ApprovalStatus.APPROVED);
        batch.setRejectionReason(null);
        batchRepository.updateApproval(batchId, ApprovalStatus.APPROVED);

        // 3. Reinsert into PriorityQueue if eligible
        if (!batch.isExpired() && batch.getAvailablePortions() > 0) {
            approvedPriorityQueue.add(batch);
        }

        return true;
    }

    public synchronized boolean rejectBatch(String batchId) {
        return rejectBatch(batchId, "Rejected by Administrator");
    }

    public synchronized boolean rejectBatch(String batchId, String reason) {
        FoodBatch batch = getBatchById(batchId);
        if (batch == null) return false;

        approvedPriorityQueue.remove(batch);
        batch.setApprovalStatus(ApprovalStatus.REJECTED);
        String finalReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Rejected by Administrator";
        batch.setRejectionReason(finalReason);
        batchRepository.updateRejection(batchId, finalReason);
        return true;
    }

    public synchronized void markExpired(String batchId) {
        FoodBatch batch = getBatchById(batchId);
        if (batch != null) {
            approvedPriorityQueue.remove(batch);
            batch.markExpired();
        }
        batchRepository.markExpired(batchId);
    }

    public synchronized void syncQueueAfterPortionChange(String batchId) {
        FoodBatch batch = getBatchById(batchId);
        if (batch == null) return;

        approvedPriorityQueue.remove(batch);
        if (batch.isApproved() && !batch.isExpired() && batch.getAvailablePortions() > 0) {
            approvedPriorityQueue.add(batch);
        }
    }
}
