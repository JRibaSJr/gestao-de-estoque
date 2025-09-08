package com.inventory.controller;

import com.inventory.dto.InventoryDTO;
import com.inventory.dto.InventoryUpdateRequest;
import com.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory Management", description = "APIs for managing inventory across distributed stores")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    @Operation(summary = "Get all inventory", description = "Retrieve complete inventory across all stores")
    public ResponseEntity<List<InventoryDTO>> getAllInventory() {
        List<InventoryDTO> inventory = inventoryService.getAllInventory();
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/store/{storeId}")
    @Operation(summary = "Get inventory by store", description = "Retrieve all inventory records for a specific store")
    public ResponseEntity<List<InventoryDTO>> getInventoryByStore(
            @Parameter(description = "Store ID", required = true)
            @PathVariable Long storeId) {
        List<InventoryDTO> inventory = inventoryService.getInventoryByStore(storeId);
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get inventory by product", description = "Retrieve inventory levels for a specific product across all stores")
    public ResponseEntity<List<InventoryDTO>> getInventoryByProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long productId) {
        List<InventoryDTO> inventory = inventoryService.getInventoryByProduct(productId);
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/store/{storeId}/product/{productId}")
    @Operation(summary = "Get specific inventory record", description = "Retrieve inventory for a specific product at a specific store")
    public ResponseEntity<InventoryDTO> getInventoryByStoreAndProduct(
            @Parameter(description = "Store ID", required = true)
            @PathVariable Long storeId,
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long productId) {
        return inventoryService.getInventoryByStoreAndProduct(storeId, productId)
                .map(inventory -> ResponseEntity.ok(inventory))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/update")
    @Operation(summary = "Update inventory", description = "Update inventory quantity with event-driven processing")
    public ResponseEntity<Map<String, String>> updateInventory(
            @Parameter(description = "Inventory update request", required = true)
            @Valid @RequestBody InventoryUpdateRequest request) {
        try {
            String result = inventoryService.updateInventory(request);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer inventory", description = "Transfer inventory between stores using saga pattern")
    public ResponseEntity<Map<String, String>> transferInventory(
            @Parameter(description = "Transfer request parameters", required = true)
            @RequestBody Map<String, Object> transferRequest) {
        try {
            Long fromStoreId = Long.valueOf(transferRequest.get("fromStoreId").toString());
            Long toStoreId = Long.valueOf(transferRequest.get("toStoreId").toString());
            Long productId = Long.valueOf(transferRequest.get("productId").toString());
            Integer quantity = Integer.valueOf(transferRequest.get("quantity").toString());
            String notes = transferRequest.get("notes") != null ? transferRequest.get("notes").toString() : "";
            
            String result = inventoryService.transferInventory(fromStoreId, toStoreId, productId, quantity, notes);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock items", description = "Retrieve inventory items below specified threshold")
    public ResponseEntity<List<InventoryDTO>> getLowStockItems(
            @Parameter(description = "Stock threshold (default: 10)")
            @RequestParam(required = false, defaultValue = "10") Integer threshold) {
        List<InventoryDTO> lowStockItems = inventoryService.getLowStockItems(threshold);
        return ResponseEntity.ok(lowStockItems);
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve inventory", description = "Reserve inventory quantity using event-driven processing")
    public ResponseEntity<Map<String, String>> reserveInventory(
            @Parameter(description = "Reservation request", required = true)
            @RequestBody Map<String, Object> reservationRequest) {
        try {
            Long storeId = Long.valueOf(reservationRequest.get("storeId").toString());
            Long productId = Long.valueOf(reservationRequest.get("productId").toString());
            Integer quantity = Integer.valueOf(reservationRequest.get("quantity").toString());
            
            String result = inventoryService.reserveInventory(storeId, productId, quantity);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/release-reservation")
    @Operation(summary = "Release reservation", description = "Release previously reserved inventory using event-driven processing")
    public ResponseEntity<Map<String, String>> releaseReservation(
            @Parameter(description = "Release request", required = true)
            @RequestBody Map<String, Object> releaseRequest) {
        try {
            Long storeId = Long.valueOf(releaseRequest.get("storeId").toString());
            Long productId = Long.valueOf(releaseRequest.get("productId").toString());
            Integer quantity = Integer.valueOf(releaseRequest.get("quantity").toString());
            
            String result = inventoryService.releaseReservation(storeId, productId, quantity);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}