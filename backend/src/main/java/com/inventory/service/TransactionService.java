package com.inventory.service;

import com.inventory.dto.TransactionDTO;
import com.inventory.model.Transaction;
import com.inventory.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    public List<TransactionDTO> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(TransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<TransactionDTO> getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .map(TransactionDTO::fromEntity);
    }

    public List<TransactionDTO> getTransactionsByStore(Long storeId) {
        return transactionRepository.findByStoreId(storeId).stream()
                .map(TransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionsByProduct(Long productId) {
        return transactionRepository.findByProductId(productId).stream()
                .map(TransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionsByType(Transaction.TransactionType type) {
        return transactionRepository.findByType(type).stream()
                .map(TransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionsByReferenceId(String referenceId) {
        return transactionRepository.findByReferenceId(referenceId).stream()
                .map(TransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByDateRange(startDate, endDate).stream()
                .map(TransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionsByStoreAndDateRange(Long storeId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByStoreIdAndDateRange(storeId, startDate, endDate).stream()
                .map(TransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> getTransactionsByProductAndDateRange(Long productId, LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByProductIdAndDateRange(productId, startDate, endDate).stream()
                .map(TransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> getRecentTransactions(int limit) {
        return transactionRepository.findAll().stream()
                .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
                .limit(limit)
                .map(TransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }
}