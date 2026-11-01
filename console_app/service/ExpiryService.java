package service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import model.FoodBatch;

/**
 * Service responsible for scanning batches against the simulated clock
 * and transitioning batches past their declared use-by time to EXPIRED.
 */
public class ExpiryService {

    /**
     * Scans the provided collection of food batches against currentTime.
     * Marks any batch with useByTime <= currentTime as expired.
     *
     * @param batches     collection of batches to evaluate
     * @param currentTime current simulated clock time
     * @return list of batches that were newly expired or are already expired
     */
    public List<FoodBatch> scanAndMarkExpired(Collection<FoodBatch> batches, LocalTime currentTime) {
        List<FoodBatch> expiredBatches = new ArrayList<>();
        if (batches == null || currentTime == null) {
            return expiredBatches;
        }

        for (FoodBatch batch : batches) {
            // If currentTime is at or after use-by time, batch is expired
            if (!currentTime.isBefore(batch.getDeclaredUseByTime())) {
                if (!batch.isExpired()) {
                    batch.markExpired();
                }
                expiredBatches.add(batch);
            }
        }
        return expiredBatches;
    }
}
