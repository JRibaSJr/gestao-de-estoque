package com.inventory.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class TransferStateService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TRANSFER_STATE_PREFIX = "transfer:state:";
    private static final String TRANSFER_LOCK_PREFIX = "transfer:lock:";

    @CachePut(value = "transfer-state", key = "#sagaId")
    public Map<String, Object> saveTransferState(String sagaId, Map<String, Object> state) {
        System.out.println("💾 Saving transfer state for saga: " + sagaId);
        
        // Also save directly to Redis with TTL
        redisTemplate.opsForValue().set(
            TRANSFER_STATE_PREFIX + sagaId, 
            state, 
            Duration.ofMinutes(30)
        );
        
        return state;
    }

    @Cacheable(value = "transfer-state", key = "#sagaId")
    public Map<String, Object> getTransferState(String sagaId) {
        System.out.println("📄 Cache MISS: Loading transfer state for saga: " + sagaId);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> state = (Map<String, Object>) redisTemplate.opsForValue()
            .get(TRANSFER_STATE_PREFIX + sagaId);
            
        return state;
    }

    @CacheEvict(value = "transfer-state", key = "#sagaId")
    public void deleteTransferState(String sagaId) {
        System.out.println("🗑️ Deleting transfer state for saga: " + sagaId);
        redisTemplate.delete(TRANSFER_STATE_PREFIX + sagaId);
    }

    public boolean acquireTransferLock(String sagaId, long timeoutSeconds) {
        String lockKey = TRANSFER_LOCK_PREFIX + sagaId;
        
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
            lockKey, 
            "locked", 
            Duration.ofSeconds(timeoutSeconds)
        );
        
        System.out.println("🔒 Transfer lock " + (Boolean.TRUE.equals(acquired) ? "ACQUIRED" : "FAILED") + " for saga: " + sagaId);
        return Boolean.TRUE.equals(acquired);
    }

    public void releaseTransferLock(String sagaId) {
        String lockKey = TRANSFER_LOCK_PREFIX + sagaId;
        redisTemplate.delete(lockKey);
        System.out.println("🔓 Transfer lock RELEASED for saga: " + sagaId);
    }

    public void updateTransferProgress(String sagaId, String step, String status, Object data) {
        Map<String, Object> currentState = getTransferState(sagaId);
        if (currentState != null) {
            currentState.put("currentStep", step);
            currentState.put("status", status);
            currentState.put("lastUpdate", System.currentTimeMillis());
            if (data != null) {
                currentState.put("stepData", data);
            }
            saveTransferState(sagaId, currentState);
        }
    }
}