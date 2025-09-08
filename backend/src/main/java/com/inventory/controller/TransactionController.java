package com.inventory.controller;

import com.inventory.dto.TransactionDTO;
import com.inventory.model.Transaction;
import com.inventory.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction History", description = "APIs for viewing inventory transaction history and audit trails")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    @Operation(summary = "Get all transactions", description = "Retrieve all inventory transactions")
    public ResponseEntity<List<TransactionDTO>> getAllTransactions() {
        List<TransactionDTO> transactions = transactionService.getAllTransactions();
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Retrieve detailed information about a specific transaction")
    public ResponseEntity<TransactionDTO> getTransactionById(
            @Parameter(description = "Transaction ID", required = true)
            @PathVariable Long id) {
        return transactionService.getTransactionById(id)
                .map(transaction -> ResponseEntity.ok(transaction))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/store/{storeId}")
    @Operation(summary = "Get transactions by store", description = "Retrieve all transactions for a specific store")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByStore(
            @Parameter(description = "Store ID", required = true)
            @PathVariable Long storeId) {
        List<TransactionDTO> transactions = transactionService.getTransactionsByStore(storeId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get transactions by product", description = "Retrieve all transactions for a specific product")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByProduct(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long productId) {
        List<TransactionDTO> transactions = transactionService.getTransactionsByProduct(productId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get transactions by type", description = "Retrieve all transactions of a specific type")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByType(
            @Parameter(description = "Transaction type", required = true)
            @PathVariable Transaction.TransactionType type) {
        List<TransactionDTO> transactions = transactionService.getTransactionsByType(type);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/reference/{referenceId}")
    @Operation(summary = "Get transactions by reference ID", description = "Retrieve all transactions with a specific reference ID")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByReferenceId(
            @Parameter(description = "Reference ID", required = true)
            @PathVariable String referenceId) {
        List<TransactionDTO> transactions = transactionService.getTransactionsByReferenceId(referenceId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get transactions by date range", description = "Retrieve transactions within a specific date range")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByDateRange(
            @Parameter(description = "Start date (yyyy-MM-dd'T'HH:mm:ss)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (yyyy-MM-dd'T'HH:mm:ss)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<TransactionDTO> transactions = transactionService.getTransactionsByDateRange(startDate, endDate);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/store/{storeId}/date-range")
    @Operation(summary = "Get store transactions by date range", description = "Retrieve transactions for a specific store within a date range")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByStoreAndDateRange(
            @Parameter(description = "Store ID", required = true)
            @PathVariable Long storeId,
            @Parameter(description = "Start date (yyyy-MM-dd'T'HH:mm:ss)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (yyyy-MM-dd'T'HH:mm:ss)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<TransactionDTO> transactions = transactionService.getTransactionsByStoreAndDateRange(storeId, startDate, endDate);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/product/{productId}/date-range")
    @Operation(summary = "Get product transactions by date range", description = "Retrieve transactions for a specific product within a date range")
    public ResponseEntity<List<TransactionDTO>> getTransactionsByProductAndDateRange(
            @Parameter(description = "Product ID", required = true)
            @PathVariable Long productId,
            @Parameter(description = "Start date (yyyy-MM-dd'T'HH:mm:ss)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (yyyy-MM-dd'T'HH:mm:ss)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<TransactionDTO> transactions = transactionService.getTransactionsByProductAndDateRange(productId, startDate, endDate);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent transactions", description = "Retrieve the most recent transactions")
    public ResponseEntity<List<TransactionDTO>> getRecentTransactions(
            @Parameter(description = "Number of transactions to retrieve (default: 50)")
            @RequestParam(required = false, defaultValue = "50") int limit) {
        List<TransactionDTO> transactions = transactionService.getRecentTransactions(limit);
        return ResponseEntity.ok(transactions);
    }
}