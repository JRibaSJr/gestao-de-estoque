package com.inventory.service;

import com.inventory.model.SyncEvent;
import com.inventory.model.Store;
import com.inventory.repository.SyncEventRepository;
import com.inventory.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@Transactional
public class SyncService {

    @Autowired
    private SyncEventRepository syncEventRepository;
    
    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private StoreService storeService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Async
    public void triggerSync(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));
        
        SyncEvent syncEvent = new SyncEvent(store, SyncEvent.EventType.SYNC_REQUEST, 
                "{\"triggered_at\":\"" + LocalDateTime.now() + "\"}");
        syncEvent = syncEventRepository.save(syncEvent);
        
        try {
            // Simulate sync process
            processSyncEvent(syncEvent);
        } catch (Exception e) {
            handleSyncFailure(syncEvent, e);
        }
    }

    @Async
    public void triggerGlobalSync() {
        List<Store> stores = storeRepository.findByStatus(Store.StoreStatus.ACTIVE);
        
        for (Store store : stores) {
            triggerSync(store.getId());
        }
    }

    private void processSyncEvent(SyncEvent syncEvent) {
        try {
            syncEvent.setStatus(SyncEvent.EventStatus.PROCESSING);
            syncEventRepository.save(syncEvent);
            
            // Simulate processing time
            Thread.sleep(1000 + (long)(Math.random() * 2000));
            
            // Update store sync status
            if (syncEvent.getStore() != null) {
                storeService.updateSyncStatus(syncEvent.getStore().getId());
            }
            
            // Mark as completed
            syncEvent.setStatus(SyncEvent.EventStatus.COMPLETED);
            syncEvent.setProcessedAt(LocalDateTime.now());
            syncEventRepository.save(syncEvent);
            
            // Send real-time update
            messagingTemplate.convertAndSend("/topic/sync-updates", getSyncStatus());
            
        } catch (Exception e) {
            handleSyncFailure(syncEvent, e);
        }
    }

    private void handleSyncFailure(SyncEvent syncEvent, Exception e) {
        syncEvent.setStatus(SyncEvent.EventStatus.FAILED);
        syncEvent.setErrorMessage(e.getMessage());
        syncEvent.setRetryCount(syncEvent.getRetryCount() + 1);
        syncEvent.setProcessedAt(LocalDateTime.now());
        syncEventRepository.save(syncEvent);
        
        // Update store status to sync error if retries exceeded
        if (syncEvent.getRetryCount() >= 3 && syncEvent.getStore() != null) {
            storeService.updateStoreStatus(syncEvent.getStore().getId(), Store.StoreStatus.SYNC_ERROR);
        }
        
        // Send error notification
        messagingTemplate.convertAndSend("/topic/sync-errors", 
                Map.of("storeId", syncEvent.getStore() != null ? syncEvent.getStore().getId() : null,
                       "error", e.getMessage(),
                       "timestamp", LocalDateTime.now()));
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void retryFailedSyncs() {
        List<SyncEvent> failedEvents = syncEventRepository.findPendingEvents(SyncEvent.EventStatus.FAILED, 3);
        
        for (SyncEvent event : failedEvents) {
            if (event.getRetryCount() < 3) {
                event.setStatus(SyncEvent.EventStatus.RETRY);
                syncEventRepository.save(event);
                processSyncEvent(event);
            }
        }
    }

    public Map<String, Object> getSyncStatus() {
        Map<String, Object> status = new HashMap<>();
        
        long pendingCount = syncEventRepository.countByStatus(SyncEvent.EventStatus.PENDING);
        long processingCount = syncEventRepository.countByStatus(SyncEvent.EventStatus.PROCESSING);
        long completedCount = syncEventRepository.countByStatus(SyncEvent.EventStatus.COMPLETED);
        long failedCount = syncEventRepository.countByStatus(SyncEvent.EventStatus.FAILED);
        
        status.put("pending", pendingCount);
        status.put("processing", processingCount);
        status.put("completed", completedCount);
        status.put("failed", failedCount);
        status.put("timestamp", LocalDateTime.now());
        
        int storesNeedingSyncCount = storeService.getStoresNeedingSync().size();
        status.put("storesNeedingSync", storesNeedingSyncCount);
        
        return status;
    }

    public List<SyncEvent> getRecentSyncEvents(int limit) {
        return syncEventRepository.findAll().stream()
                .sorted((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()))
                .limit(limit)
                .toList();
    }

    public List<SyncEvent> getSyncEventsByStore(Long storeId) {
        return syncEventRepository.findByStoreId(storeId);
    }
}