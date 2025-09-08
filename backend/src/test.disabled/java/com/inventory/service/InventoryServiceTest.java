package com.inventory.service;

import com.inventory.dto.InventoryDTO;
import com.inventory.dto.InventoryUpdateRequest;
import com.inventory.model.Inventory;
import com.inventory.model.Store;
import com.inventory.model.Product;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.StoreRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.publisher.InventoryEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;
    
    @Mock
    private StoreRepository storeRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private InventoryEventPublisher eventPublisher;
    
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InventoryService inventoryService;

    private Store mockStore;
    private Product mockProduct;
    private Inventory mockInventory;

    @BeforeEach
    void setUp() {
        mockStore = new Store();
        mockStore.setId(1L);
        mockStore.setName("Test Store");

        mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setName("Test Product");

        mockInventory = new Inventory(mockStore, mockProduct, 100);
        mockInventory.setId(1L);
        mockInventory.setVersion(1L);
    }

    @Test
    void testGetAllInventory_Success() {
        // Arrange
        List<Inventory> inventories = Arrays.asList(mockInventory);
        when(inventoryRepository.findAll()).thenReturn(inventories);

        // Act
        List<InventoryDTO> result = inventoryService.getAllInventory();

        // Assert
        assertEquals(1, result.size());
        assertEquals(100, result.get(0).getQuantity());
        verify(inventoryRepository).findAll();
    }

    @Test
    void testStockIn_Success() {
        // Arrange
        when(storeRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(eventPublisher).publishInventoryUpdate(any());
        doNothing().when(notificationService).notifyOperationSuccess(anyString(), anyString(), any());

        // Act
        String result = inventoryService.stockIn(1L, 1L, 50, "REF-001", "Test stock in");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Stock in event published"));
        verify(storeRepository).existsById(1L);
        verify(productRepository).existsById(1L);
        verify(eventPublisher).publishInventoryUpdate(any());
        verify(notificationService).notifyOperationSuccess(eq("STOCK_IN"), anyString(), any());
    }

    @Test
    void testStockIn_InvalidQuantity() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> inventoryService.stockIn(1L, 1L, -10, "REF-001", "Invalid quantity"));
        
        assertTrue(exception.getMessage().contains("Quantity must be positive"));
        verify(eventPublisher, never()).publishInventoryUpdate(any());
    }

    @Test
    void testStockOut_InsufficientStock() {
        // Arrange
        when(storeRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(1L)).thenReturn(true);
        
        Inventory lowStockInventory = new Inventory(mockStore, mockProduct, 5);
        when(inventoryRepository.findByStoreIdAndProductId(1L, 1L))
            .thenReturn(Optional.of(lowStockInventory));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> inventoryService.stockOut(1L, 1L, 10, "REF-002", "Insufficient stock test"));
        
        assertTrue(exception.getMessage().contains("Insufficient stock"));
        verify(eventPublisher, never()).publishInventoryUpdate(any());
    }

    @Test
    void testUpdateInventory_StoreNotFound() {
        // Arrange
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setStoreId(999L);
        request.setProductId(1L);
        request.setQuantityAdjustment(10);
        
        when(storeRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> inventoryService.updateInventory(request));
        
        assertTrue(exception.getMessage().contains("Store not found"));
    }

    @Test
    void testGetInventoryByStoreAndProduct_Found() {
        // Arrange
        when(inventoryRepository.findByStoreIdAndProductId(1L, 1L))
            .thenReturn(Optional.of(mockInventory));

        // Act
        Optional<InventoryDTO> result = inventoryService.getInventoryByStoreAndProduct(1L, 1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(100, result.get().getQuantity());
        assertEquals(1L, result.get().getVersion());
    }

    @Test
    void testTransferInventory_Success() {
        // Arrange
        when(storeRepository.existsById(1L)).thenReturn(true);
        when(storeRepository.existsById(2L)).thenReturn(true);
        when(productRepository.existsById(1L)).thenReturn(true);
        doNothing().when(eventPublisher).publishInventoryTransfer(any());

        // Act
        String result = inventoryService.transferInventory(1L, 2L, 1L, 20, "Transfer test");

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Inventory transfer started with saga ID"));
        verify(eventPublisher).publishInventoryTransfer(any());
    }
}