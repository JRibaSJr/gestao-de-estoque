package com.inventory.config;

import com.inventory.model.Store;
import com.inventory.model.Product;
import com.inventory.model.Inventory;
import com.inventory.model.Transaction;
import com.inventory.repository.StoreRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.repository.InventoryRepository;
import com.inventory.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private StoreRepository storeRepository;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private TransactionRepository transactionRepository;

    private Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        if (storeRepository.count() == 0) {
            initializeSampleData();
        }
    }

    private void initializeSampleData() {
        System.out.println("🚀 Initializing comprehensive sample data...");
        
        // Create Stores (8 stores with different characteristics)
        List<Store> stores = createStores();
        storeRepository.saveAll(stores);
        
        // Create Products (25 products across multiple categories)
        List<Product> products = createProducts();
        productRepository.saveAll(products);
        
        // Create Inventory distribution
        createInventoryRecords(stores, products);
        
        // Create Transaction history
        createTransactionHistory(stores, products);
        
        System.out.println("✅ Comprehensive sample data initialized successfully!");
        System.out.println("📊 Created " + stores.size() + " stores, " + products.size() + " products");
        System.out.println("📦 Generated inventory records and transaction history");
        System.out.println("🎯 System ready for testing with realistic data scenarios!");
    }

    private List<Store> createStores() {
        return Arrays.asList(
            createStore("Loja Centro SP", "São Paulo - Centro", Store.StoreStatus.ACTIVE, 30),
            createStore("Shopping Vila Olímpia", "São Paulo - Vila Olímpia", Store.StoreStatus.ACTIVE, 45),
            createStore("Loja Santana", "São Paulo - Santana", Store.StoreStatus.ACTIVE, 60),
            createStore("Outlet Marginal", "São Paulo - Marginal Pinheiros", Store.StoreStatus.MAINTENANCE, 120),
            createStore("Loja Rio Copacabana", "Rio de Janeiro - Copacabana", Store.StoreStatus.ACTIVE, 15),
            createStore("Shopping Barra RJ", "Rio de Janeiro - Barra da Tijuca", Store.StoreStatus.ACTIVE, 25),
            createStore("Loja BH Centro", "Belo Horizonte - Centro", Store.StoreStatus.ACTIVE, 90),
            createStore("Loja Porto Alegre", "Porto Alegre - Centro Histórico", Store.StoreStatus.INACTIVE, 480)
        );
    }

    private Store createStore(String name, String location, Store.StoreStatus status, int minutesAgo) {
        Store store = new Store(name, location);
        store.setStatus(status);
        store.setLastSync(LocalDateTime.now().minusMinutes(minutesAgo));
        return store;
    }

    private List<Product> createProducts() {
        return Arrays.asList(
            // Smartphones & Tablets
            createProduct("iPhone 15 Pro", "Apple iPhone 15 Pro 256GB Titânio Natural", "Smartphones", "4999.99", "IPHONE15-PRO-256"),
            createProduct("iPhone 15", "Apple iPhone 15 128GB Azul", "Smartphones", "4299.99", "IPHONE15-128-BLUE"),
            createProduct("Samsung Galaxy S24 Ultra", "Samsung Galaxy S24 Ultra 512GB Violeta", "Smartphones", "5199.99", "GALAXY-S24-ULTRA-512"),
            createProduct("Samsung Galaxy S24", "Samsung Galaxy S24 256GB Preto", "Smartphones", "3899.99", "GALAXY-S24-256-BLACK"),
            createProduct("iPad Pro 12.9", "Apple iPad Pro 12.9 M2 256GB Wi-Fi", "Tablets", "7499.99", "IPAD-PRO-129-256"),
            createProduct("iPad Air", "Apple iPad Air 10.9 128GB Wi-Fi Azul", "Tablets", "3999.99", "IPAD-AIR-109-128"),
            
            // Notebooks & Computadores
            createProduct("MacBook Pro 14", "Apple MacBook Pro 14 M3 Pro 512GB Space Black", "Notebooks", "14999.99", "MBP-14-M3PRO-512"),
            createProduct("MacBook Air M2", "Apple MacBook Air M2 256GB Midnight", "Notebooks", "8999.99", "MBA-M2-256-MIDNIGHT"),
            createProduct("Dell XPS 13", "Dell XPS 13 Intel i7 16GB 512GB", "Notebooks", "6799.99", "DELL-XPS13-I7-512"),
            createProduct("Lenovo ThinkPad X1", "Lenovo ThinkPad X1 Carbon Intel i7 32GB 1TB", "Notebooks", "8999.99", "LENOVO-X1-I7-1TB"),
            createProduct("Mac Mini M2", "Apple Mac Mini M2 256GB", "Desktops", "3699.99", "MAC-MINI-M2-256"),
            
            // Acessórios
            createProduct("AirPods Pro 2", "Apple AirPods Pro 2ª Geração com Case MagSafe", "Acessórios", "1899.99", "AIRPODS-PRO-2"),
            createProduct("AirPods 3", "Apple AirPods 3ª Geração", "Acessórios", "1299.99", "AIRPODS-3"),
            createProduct("Magic Keyboard", "Apple Magic Keyboard para iPad Pro 12.9", "Acessórios", "2299.99", "MAGIC-KB-IPAD-PRO"),
            createProduct("Apple Pencil 2", "Apple Pencil 2ª Geração", "Acessórios", "899.99", "APPLE-PENCIL-2"),
            createProduct("Samsung Galaxy Buds2 Pro", "Samsung Galaxy Buds2 Pro Grafite", "Acessórios", "799.99", "GALAXY-BUDS2-PRO"),
            
            // Gaming & Entretenimento
            createProduct("PlayStation 5", "Sony PlayStation 5 Digital Edition", "Games", "2999.99", "PS5-DIGITAL"),
            createProduct("Xbox Series X", "Microsoft Xbox Series X 1TB", "Games", "3199.99", "XBOX-SERIES-X"),
            createProduct("Nintendo Switch OLED", "Nintendo Switch OLED Neon Red/Blue", "Games", "2199.99", "SWITCH-OLED-NEON"),
            
            // TVs & Monitores
            createProduct("Apple TV 4K", "Apple TV 4K 128GB 3ª Geração", "TV & Audio", "1399.99", "APPLE-TV-4K-128"),
            createProduct("Samsung 4K 55", "Samsung Smart TV 55 4K QLED", "TV & Audio", "2799.99", "SAMSUNG-55-4K-QLED"),
            createProduct("LG OLED 65", "LG OLED 65 4K Smart TV", "TV & Audio", "6999.99", "LG-OLED-65-4K"),
            
            // Wearables
            createProduct("Apple Watch Ultra 2", "Apple Watch Ultra 2 GPS+Cellular 49mm", "Wearables", "5299.99", "WATCH-ULTRA-2-49"),
            createProduct("Apple Watch Series 9", "Apple Watch Series 9 GPS 45mm", "Wearables", "2699.99", "WATCH-S9-45-GPS"),
            createProduct("Samsung Galaxy Watch6", "Samsung Galaxy Watch6 Classic 47mm", "Wearables", "1999.99", "GALAXY-WATCH6-47")
        );
    }

    private Product createProduct(String name, String description, String category, String price, String sku) {
        return new Product(name, description, category, new BigDecimal(price), sku);
    }

    private void createInventoryRecords(List<Store> stores, List<Product> products) {
        int inventoryRecordsCreated = 0;
        
        for (Store store : stores) {
            for (Product product : products) {
                // Skip some products for some stores to create realistic distribution
                if (random.nextDouble() < 0.15) continue; // 15% chance to skip
                
                int quantity = generateRealisticQuantity(store, product);
                
                Inventory inventory = new Inventory(store, product, quantity);
                
                // Add some reservations randomly
                if (quantity > 5 && random.nextDouble() < 0.3) {
                    int reservedAmount = random.nextInt(Math.min(quantity / 2, 5)) + 1;
                    inventory.setReservedQuantity(reservedAmount);
                }
                
                inventoryRepository.save(inventory);
                inventoryRecordsCreated++;
            }
        }
        
        System.out.println("📦 Created " + inventoryRecordsCreated + " inventory records");
    }

    private int generateRealisticQuantity(Store store, Product product) {
        // Different quantity ranges based on store status and product category
        int baseQuantity;
        
        switch (store.getStatus()) {
            case ACTIVE:
                baseQuantity = random.nextInt(50) + 10; // 10-59
                break;
            case MAINTENANCE:
                baseQuantity = random.nextInt(20) + 5; // 5-24
                break;
            case INACTIVE:
                baseQuantity = random.nextInt(10); // 0-9
                break;
            default:
                baseQuantity = random.nextInt(30) + 5; // 5-34
        }
        
        // Adjust based on product category
        String category = product.getCategory();
        switch (category) {
            case "Smartphones":
            case "Tablets":
                return Math.max(0, baseQuantity - random.nextInt(10)); // Tend to have less stock
            case "Notebooks":
            case "Desktops":
                return Math.max(0, baseQuantity - random.nextInt(20)); // Even less stock for expensive items
            case "Acessórios":
                return baseQuantity + random.nextInt(30); // More accessories in stock
            case "Games":
                return Math.max(0, baseQuantity - random.nextInt(15));
            default:
                return baseQuantity;
        }
    }

    private void createTransactionHistory(List<Store> stores, List<Product> products) {
        int transactionsCreated = 0;
        LocalDateTime now = LocalDateTime.now();
        
        // Create transactions for the last 30 days
        for (int day = 30; day >= 0; day--) {
            LocalDateTime transactionDate = now.minusDays(day);
            
            // Create 5-15 random transactions per day
            int dailyTransactions = random.nextInt(11) + 5;
            
            for (int i = 0; i < dailyTransactions; i++) {
                Store randomStore = stores.get(random.nextInt(stores.size()));
                Product randomProduct = products.get(random.nextInt(products.size()));
                
                // Random transaction type
                Transaction.TransactionType[] types = Transaction.TransactionType.values();
                Transaction.TransactionType type = types[random.nextInt(types.length)];
                
                int quantity = random.nextInt(10) + 1;
                String referenceId = generateReferenceId(type);
                String notes = generateTransactionNotes(type);
                
                Transaction transaction = new Transaction(
                    randomStore, 
                    randomProduct, 
                    type, 
                    quantity, 
                    referenceId, 
                    notes
                );
                
                // Set random time within the day
                transaction.setTimestamp(transactionDate
                    .plusHours(random.nextInt(24))
                    .plusMinutes(random.nextInt(60)));
                
                transactionRepository.save(transaction);
                transactionsCreated++;
            }
        }
        
        System.out.println("📊 Created " + transactionsCreated + " transaction history records");
    }

    private String generateReferenceId(Transaction.TransactionType type) {
        String prefix;
        switch (type) {
            case STOCK_IN:
                prefix = "PO"; // Purchase Order
                break;
            case STOCK_OUT:
                prefix = "SALE";
                break;
            case TRANSFER_IN:
            case TRANSFER_OUT:
                prefix = "TRF";
                break;
            case ADJUSTMENT:
                prefix = "ADJ";
                break;
            case RESERVATION:
                prefix = "RES";
                break;
            case RELEASE:
                prefix = "REL";
                break;
            default:
                prefix = "TXN";
        }
        
        return prefix + "-" + (2024000000L + random.nextInt(999999));
    }

    private String generateTransactionNotes(Transaction.TransactionType type) {
        String[] stockInNotes = {
            "Recebimento de fornecedor", "Reposição de estoque", "Chegada de nova remessa",
            "Transferência de CD", "Devolução de cliente", "Estoque inicial"
        };
        
        String[] stockOutNotes = {
            "Venda no balcão", "Venda online", "Demonstração", "Perda/avaria",
            "Transferência para outra loja", "Devolução para fornecedor"
        };
        
        String[] transferNotes = {
            "Rebalanceamento de estoque", "Atendimento de demanda", "Redistribuição regional",
            "Transferência de emergência", "Otimização de inventário"
        };
        
        String[] adjustmentNotes = {
            "Acerto de inventário", "Correção de sistema", "Ajuste após auditoria",
            "Diferença de contagem", "Correção manual"
        };
        
        switch (type) {
            case STOCK_IN:
                return stockInNotes[random.nextInt(stockInNotes.length)];
            case STOCK_OUT:
                return stockOutNotes[random.nextInt(stockOutNotes.length)];
            case TRANSFER_IN:
            case TRANSFER_OUT:
                return transferNotes[random.nextInt(transferNotes.length)];
            case ADJUSTMENT:
                return adjustmentNotes[random.nextInt(adjustmentNotes.length)];
            case RESERVATION:
                return "Reserva para cliente";
            case RELEASE:
                return "Liberação de reserva";
            default:
                return "Transação automática do sistema";
        }
    }
}