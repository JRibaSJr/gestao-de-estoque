package com.inventory.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public enum NotificationType {
        STOCK_LOW, STOCK_OUT, TRANSFER_COMPLETE, TRANSFER_FAILED, SYSTEM_ERROR, OPERATION_SUCCESS
    }

    public void notifyStockLow(Long storeId, Long productId, Integer currentStock, Integer threshold) {
        Map<String, Object> notification = createNotification(
            NotificationType.STOCK_LOW,
            "Estoque Baixo",
            String.format("Produto %d na loja %d está com estoque baixo: %d (limite: %d)", 
                productId, storeId, currentStock, threshold),
            Map.of("storeId", storeId, "productId", productId, "currentStock", currentStock)
        );
        
        sendNotification("/topic/inventory-alerts", notification);
        sendNotification("/topic/store/" + storeId + "/alerts", notification);
    }

    public void notifyStockOut(Long storeId, Long productId) {
        Map<String, Object> notification = createNotification(
            NotificationType.STOCK_OUT,
            "Estoque Zerado",
            String.format("Produto %d na loja %d está sem estoque", productId, storeId),
            Map.of("storeId", storeId, "productId", productId)
        );
        
        sendNotification("/topic/inventory-alerts", notification);
        sendNotification("/topic/store/" + storeId + "/alerts", notification);
    }

    public void notifyTransferComplete(String sagaId, Long fromStore, Long toStore, Long productId, Integer quantity) {
        Map<String, Object> notification = createNotification(
            NotificationType.TRANSFER_COMPLETE,
            "Transferência Concluída",
            String.format("Transferência de %d unidades do produto %d da loja %d para loja %d concluída", 
                quantity, productId, fromStore, toStore),
            Map.of("sagaId", sagaId, "fromStore", fromStore, "toStore", toStore, "productId", productId, "quantity", quantity)
        );
        
        sendNotification("/topic/transfer-updates", notification);
        sendNotification("/topic/store/" + fromStore + "/notifications", notification);
        sendNotification("/topic/store/" + toStore + "/notifications", notification);
    }

    public void notifyTransferFailed(String sagaId, Long fromStore, Long toStore, Long productId, String reason) {
        Map<String, Object> notification = createNotification(
            NotificationType.TRANSFER_FAILED,
            "Transferência Falhou",
            String.format("Transferência do produto %d da loja %d para loja %d falhou: %s", 
                productId, fromStore, toStore, reason),
            Map.of("sagaId", sagaId, "fromStore", fromStore, "toStore", toStore, "productId", productId, "reason", reason)
        );
        
        sendNotification("/topic/transfer-updates", notification);
        sendNotification("/topic/store/" + fromStore + "/notifications", notification);
    }

    public void notifyOperationSuccess(String operation, String message, Map<String, Object> data) {
        Map<String, Object> notification = createNotification(
            NotificationType.OPERATION_SUCCESS,
            "Operação Concluída",
            message,
            data
        );
        
        sendNotification("/topic/system-notifications", notification);
    }

    public void notifySystemError(String operation, String error, Map<String, Object> context) {
        Map<String, Object> notification = createNotification(
            NotificationType.SYSTEM_ERROR,
            "Erro do Sistema",
            String.format("Erro na operação %s: %s", operation, error),
            context
        );
        
        sendNotification("/topic/system-alerts", notification);
    }

    private Map<String, Object> createNotification(NotificationType type, String title, String message, Map<String, Object> data) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("id", System.currentTimeMillis());
        notification.put("type", type.name());
        notification.put("title", title);
        notification.put("message", message);
        notification.put("timestamp", LocalDateTime.now());
        notification.put("data", data);
        notification.put("read", false);
        
        return notification;
    }

    private void sendNotification(String destination, Map<String, Object> notification) {
        try {
            messagingTemplate.convertAndSend(destination, notification);
            System.out.println("📢 Notificação enviada: " + destination + " - " + notification.get("title"));
        } catch (Exception e) {
            System.err.println("❌ Erro ao enviar notificação: " + e.getMessage());
        }
    }
}