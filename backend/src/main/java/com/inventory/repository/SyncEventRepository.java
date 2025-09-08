package com.inventory.repository;

import com.inventory.model.SyncEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SyncEventRepository extends JpaRepository<SyncEvent, Long> {
    
    List<SyncEvent> findByStoreId(Long storeId);
    
    List<SyncEvent> findByStatus(SyncEvent.EventStatus status);
    
    List<SyncEvent> findByEventType(SyncEvent.EventType eventType);
    
    @Query("SELECT s FROM SyncEvent s WHERE s.status = :status AND s.retryCount < :maxRetries ORDER BY s.timestamp ASC")
    List<SyncEvent> findPendingEvents(@Param("status") SyncEvent.EventStatus status, @Param("maxRetries") Integer maxRetries);
    
    @Query("SELECT s FROM SyncEvent s WHERE s.timestamp BETWEEN :startDate AND :endDate ORDER BY s.timestamp DESC")
    List<SyncEvent> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT s FROM SyncEvent s WHERE s.store.id = :storeId AND s.timestamp BETWEEN :startDate AND :endDate ORDER BY s.timestamp DESC")
    List<SyncEvent> findByStoreIdAndDateRange(@Param("storeId") Long storeId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(s) FROM SyncEvent s WHERE s.status = :status")
    long countByStatus(@Param("status") SyncEvent.EventStatus status);
    
    @Query("SELECT s FROM SyncEvent s WHERE s.status = 'FAILED' AND s.retryCount >= :maxRetries")
    List<SyncEvent> findFailedEvents(@Param("maxRetries") Integer maxRetries);
}