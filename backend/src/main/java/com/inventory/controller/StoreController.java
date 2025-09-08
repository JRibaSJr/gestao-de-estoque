package com.inventory.controller;

import com.inventory.dto.StoreDTO;
import com.inventory.model.Store;
import com.inventory.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@Tag(name = "Store Management", description = "APIs for managing stores in the distributed inventory system")
public class StoreController {

    @Autowired
    private StoreService storeService;

    @GetMapping
    @Operation(summary = "Get all stores", description = "Retrieve a list of all stores with their current status and inventory summary")
    public ResponseEntity<List<StoreDTO>> getAllStores() {
        List<StoreDTO> stores = storeService.getAllStores();
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get store by ID", description = "Retrieve detailed information about a specific store")
    public ResponseEntity<StoreDTO> getStoreById(
            @Parameter(description = "Store ID", required = true)
            @PathVariable Long id) {
        return storeService.getStoreById(id)
                .map(store -> ResponseEntity.ok(store))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new store", description = "Create a new store in the system")
    public ResponseEntity<StoreDTO> createStore(
            @Parameter(description = "Store information", required = true)
            @Valid @RequestBody StoreDTO storeDTO) {
        try {
            StoreDTO createdStore = storeService.createStore(storeDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdStore);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update store", description = "Update an existing store's information")
    public ResponseEntity<StoreDTO> updateStore(
            @Parameter(description = "Store ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated store information", required = true)
            @Valid @RequestBody StoreDTO storeDTO) {
        return storeService.updateStore(id, storeDTO)
                .map(store -> ResponseEntity.ok(store))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete store", description = "Delete a store from the system")
    public ResponseEntity<Void> deleteStore(
            @Parameter(description = "Store ID", required = true)
            @PathVariable Long id) {
        boolean deleted = storeService.deleteStore(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get stores by status", description = "Retrieve all stores with a specific status")
    public ResponseEntity<List<StoreDTO>> getStoresByStatus(
            @Parameter(description = "Store status", required = true)
            @PathVariable Store.StoreStatus status) {
        List<StoreDTO> stores = storeService.getStoresByStatus(status);
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/search")
    @Operation(summary = "Search stores by location", description = "Search for stores by location name")
    public ResponseEntity<List<StoreDTO>> searchStoresByLocation(
            @Parameter(description = "Location search term", required = true)
            @RequestParam String location) {
        List<StoreDTO> stores = storeService.searchStoresByLocation(location);
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/sync/needed")
    @Operation(summary = "Get stores needing sync", description = "Retrieve stores that need synchronization based on last sync time")
    public ResponseEntity<List<StoreDTO>> getStoresNeedingSync() {
        List<StoreDTO> stores = storeService.getStoresNeedingSync();
        return ResponseEntity.ok(stores);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update store status", description = "Update the operational status of a store")
    public ResponseEntity<StoreDTO> updateStoreStatus(
            @Parameter(description = "Store ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "New status", required = true)
            @RequestParam Store.StoreStatus status) {
        try {
            StoreDTO updatedStore = storeService.updateStoreStatus(id, status);
            return ResponseEntity.ok(updatedStore);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/sync")
    @Operation(summary = "Update sync status", description = "Mark a store as recently synchronized")
    public ResponseEntity<StoreDTO> updateSyncStatus(
            @Parameter(description = "Store ID", required = true)
            @PathVariable Long id) {
        try {
            StoreDTO updatedStore = storeService.updateSyncStatus(id);
            return ResponseEntity.ok(updatedStore);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}