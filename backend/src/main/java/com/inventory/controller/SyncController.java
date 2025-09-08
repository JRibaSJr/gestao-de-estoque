package com.inventory.controller;

import com.inventory.model.SyncEvent;
import com.inventory.service.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@Tag(name = "Synchronization", description = "APIs for managing synchronization between distributed stores")
public class SyncController {

    @Autowired
    private SyncService syncService;

    @PostMapping("/trigger/{storeId}")
    @Operation(summary = "Trigger store sync", description = "Manually trigger synchronization for a specific store")
    public ResponseEntity<Map<String, String>> triggerSync(
            @Parameter(description = "Store ID", required = true)
            @PathVariable Long storeId) {
        try {
            syncService.triggerSync(storeId);
            return ResponseEntity.ok(Map.of("message", "Sync triggered successfully for store " + storeId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/trigger/global")
    @Operation(summary = "Trigger global sync", description = "Trigger synchronization for all active stores")
    public ResponseEntity<Map<String, String>> triggerGlobalSync() {
        syncService.triggerGlobalSync();
        return ResponseEntity.ok(Map.of("message", "Global sync triggered successfully"));
    }

    @GetMapping("/status")
    @Operation(summary = "Get sync status", description = "Retrieve current synchronization status and statistics")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        Map<String, Object> status = syncService.getSyncStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/events/recent")
    @Operation(summary = "Get recent sync events", description = "Retrieve recent synchronization events")
    public ResponseEntity<List<SyncEvent>> getRecentSyncEvents(
            @Parameter(description = "Number of events to retrieve (default: 50)")
            @RequestParam(required = false, defaultValue = "50") int limit) {
        List<SyncEvent> events = syncService.getRecentSyncEvents(limit);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/store/{storeId}")
    @Operation(summary = "Get sync events by store", description = "Retrieve synchronization events for a specific store")
    public ResponseEntity<List<SyncEvent>> getSyncEventsByStore(
            @Parameter(description = "Store ID", required = true)
            @PathVariable Long storeId) {
        List<SyncEvent> events = syncService.getSyncEventsByStore(storeId);
        return ResponseEntity.ok(events);
    }
}