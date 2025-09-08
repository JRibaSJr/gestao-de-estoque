package com.inventory.repository;

import com.inventory.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    List<Inventory> findByStoreId(Long storeId);
    
    List<Inventory> findByProductId(Long productId);
    
    Optional<Inventory> findByStoreIdAndProductId(Long storeId, Long productId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.store.id = :storeId AND i.product.id = :productId")
    Optional<Inventory> findByStoreIdAndProductIdForUpdate(@Param("storeId") Long storeId, @Param("productId") Long productId);
    
    @Query("SELECT i FROM Inventory i WHERE i.quantity < :threshold")
    List<Inventory> findLowStockItems(@Param("threshold") Integer threshold);
    
    @Query("SELECT SUM(i.quantity) FROM Inventory i WHERE i.product.id = :productId")
    Integer getTotalQuantityByProduct(@Param("productId") Long productId);
    
    @Query("SELECT SUM(i.quantity - i.reservedQuantity) FROM Inventory i WHERE i.product.id = :productId")
    Integer getAvailableQuantityByProduct(@Param("productId") Long productId);
    
    @Query("SELECT i FROM Inventory i JOIN FETCH i.store JOIN FETCH i.product WHERE i.store.id = :storeId")
    List<Inventory> findByStoreIdWithDetails(@Param("storeId") Long storeId);
}