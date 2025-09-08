package com.inventory.repository;

import com.inventory.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    
    List<Store> findByStatus(Store.StoreStatus status);
    
    List<Store> findByLocationContainingIgnoreCase(String location);
    
    Optional<Store> findByNameIgnoreCase(String name);
    
    @Query("SELECT s FROM Store s WHERE s.lastSync < :threshold OR s.lastSync IS NULL")
    List<Store> findStoresNeedingSync(@Param("threshold") LocalDateTime threshold);
    
    @Query("SELECT COUNT(s) FROM Store s WHERE s.status = :status")
    long countByStatus(@Param("status") Store.StoreStatus status);
    
    @Query("SELECT s FROM Store s LEFT JOIN FETCH s.inventories WHERE s.id = :id")
    Optional<Store> findByIdWithInventories(@Param("id") Long id);
}