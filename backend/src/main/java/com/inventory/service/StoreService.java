package com.inventory.service;

import com.inventory.dto.StoreDTO;
import com.inventory.model.Store;
import com.inventory.repository.StoreRepository;
import com.inventory.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class StoreService {

    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private InventoryRepository inventoryRepository;

    public List<StoreDTO> getAllStores() {
        return storeRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Optional<StoreDTO> getStoreById(Long id) {
        return storeRepository.findById(id)
                .map(this::convertToDTO);
    }

    public StoreDTO createStore(StoreDTO storeDTO) {
        Store store = convertToEntity(storeDTO);
        store = storeRepository.save(store);
        return convertToDTO(store);
    }

    public Optional<StoreDTO> updateStore(Long id, StoreDTO storeDTO) {
        return storeRepository.findById(id)
                .map(store -> {
                    store.setName(storeDTO.getName());
                    store.setLocation(storeDTO.getLocation());
                    store.setStatus(storeDTO.getStatus());
                    return convertToDTO(storeRepository.save(store));
                });
    }

    public boolean deleteStore(Long id) {
        if (storeRepository.existsById(id)) {
            storeRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<StoreDTO> getStoresByStatus(Store.StoreStatus status) {
        return storeRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StoreDTO> searchStoresByLocation(String location) {
        return storeRepository.findByLocationContainingIgnoreCase(location).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StoreDTO> getStoresNeedingSync() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        return storeRepository.findStoresNeedingSync(threshold).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public StoreDTO updateSyncStatus(Long storeId) {
        return storeRepository.findById(storeId)
                .map(store -> {
                    store.setLastSync(LocalDateTime.now());
                    if (store.getStatus() == Store.StoreStatus.SYNC_ERROR) {
                        store.setStatus(Store.StoreStatus.ACTIVE);
                    }
                    return convertToDTO(storeRepository.save(store));
                })
                .orElseThrow(() -> new RuntimeException("Store not found with id: " + storeId));
    }

    public StoreDTO updateStoreStatus(Long storeId, Store.StoreStatus status) {
        return storeRepository.findById(storeId)
                .map(store -> {
                    store.setStatus(status);
                    return convertToDTO(storeRepository.save(store));
                })
                .orElseThrow(() -> new RuntimeException("Store not found with id: " + storeId));
    }

    private StoreDTO convertToDTO(Store store) {
        StoreDTO dto = StoreDTO.fromEntity(store);
        
        // Add additional computed fields
        long productCount = inventoryRepository.findByStoreId(store.getId()).size();
        long totalQuantity = inventoryRepository.findByStoreId(store.getId())
                .stream()
                .mapToLong(inventory -> inventory.getQuantity())
                .sum();
        
        dto.setTotalProducts(productCount);
        dto.setTotalQuantity(totalQuantity);
        
        return dto;
    }

    private Store convertToEntity(StoreDTO dto) {
        Store store = new Store();
        store.setName(dto.getName());
        store.setLocation(dto.getLocation());
        store.setStatus(dto.getStatus() != null ? dto.getStatus() : Store.StoreStatus.ACTIVE);
        return store;
    }
}