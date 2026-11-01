package com.annam.service;

import com.annam.model.FoodBatch;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExpiryService {
    private final FoodBatchService foodBatchService;
    private final SimulatedClockService clockService;

    public ExpiryService(FoodBatchService foodBatchService, SimulatedClockService clockService) {
        this.foodBatchService = foodBatchService;
        this.clockService = clockService;
    }

    public synchronized List<String> runExpiryCheck() {
        LocalTime now = clockService.getCurrentTime();
        List<FoodBatch> all = foodBatchService.getAllBatches();
        List<String> expiredBatchIds = new ArrayList<>();

        for (FoodBatch batch : all) {
            if (!batch.isExpired()) {
                // If current simulated time is at or after use-by time
                if (!now.isBefore(batch.getDeclaredUseByTime())) {
                    foodBatchService.markExpired(batch.getBatchId());
                    expiredBatchIds.add(batch.getBatchId());
                }
            }
        }

        return expiredBatchIds;
    }
}
