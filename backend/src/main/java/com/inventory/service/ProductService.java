package com.inventory.service;

import com.inventory.dto.ProductDTO;
import com.inventory.model.Product;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private InventoryRepository inventoryRepository;

    @Cacheable(value = "products", key = "'all'")
    public List<ProductDTO> getAllProducts() {
        System.out.println("🛍️ Cache MISS: Loading all products from database");
        return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "products", key = "'id:' + #id")
    public Optional<ProductDTO> getProductById(Long id) {
        System.out.println("🛍️ Cache MISS: Loading product " + id + " from database");
        return productRepository.findById(id)
                .map(this::convertToDTO);
    }

    @Cacheable(value = "products", key = "'sku:' + #sku")
    public Optional<ProductDTO> getProductBySku(String sku) {
        System.out.println("🛍️ Cache MISS: Loading product with SKU " + sku + " from database");
        return productRepository.findBySku(sku)
                .map(this::convertToDTO);
    }

    public ProductDTO createProduct(ProductDTO productDTO) {
        // Check if SKU already exists
        if (productRepository.findBySku(productDTO.getSku()).isPresent()) {
            throw new RuntimeException("Product with SKU " + productDTO.getSku() + " already exists");
        }
        
        Product product = convertToEntity(productDTO);
        product = productRepository.save(product);
        return convertToDTO(product);
    }

    public Optional<ProductDTO> updateProduct(Long id, ProductDTO productDTO) {
        return productRepository.findById(id)
                .map(product -> {
                    // Check if new SKU conflicts with existing product
                    if (!product.getSku().equals(productDTO.getSku())) {
                        Optional<Product> existingProduct = productRepository.findBySku(productDTO.getSku());
                        if (existingProduct.isPresent() && !existingProduct.get().getId().equals(id)) {
                            throw new RuntimeException("Product with SKU " + productDTO.getSku() + " already exists");
                        }
                    }
                    
                    product.setName(productDTO.getName());
                    product.setDescription(productDTO.getDescription());
                    product.setCategory(productDTO.getCategory());
                    product.setPrice(productDTO.getPrice());
                    product.setSku(productDTO.getSku());
                    
                    return convertToDTO(productRepository.save(product));
                });
    }

    public boolean deleteProduct(Long id) {
        if (productRepository.existsById(id)) {
            // Check if product has inventory records
            List<com.inventory.model.Inventory> inventories = inventoryRepository.findByProductId(id);
            if (!inventories.isEmpty()) {
                throw new RuntimeException("Cannot delete product with existing inventory records");
            }
            
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<ProductDTO> getProductsByCategory(String category) {
        return productRepository.findByCategory(category).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductDTO> searchProducts(String searchTerm) {
        return productRepository.searchProducts(searchTerm).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "products", key = "'categories'")
    public List<String> getAllCategories() {
        System.out.println("🏷️ Cache MISS: Loading product categories from database");
        return productRepository.findAllCategories();
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = ProductDTO.fromEntity(product);
        
        // Add computed fields
        Integer totalQuantity = inventoryRepository.getTotalQuantityByProduct(product.getId());
        Integer availableQuantity = inventoryRepository.getAvailableQuantityByProduct(product.getId());
        long storeCount = inventoryRepository.findByProductId(product.getId()).size();
        
        dto.setTotalQuantity(totalQuantity != null ? totalQuantity : 0);
        dto.setAvailableQuantity(availableQuantity != null ? availableQuantity : 0);
        dto.setStoreCount(storeCount);
        
        return dto;
    }

    private Product convertToEntity(ProductDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setCategory(dto.getCategory());
        product.setPrice(dto.getPrice());
        product.setSku(dto.getSku());
        return product;
    }
}