#!/usr/bin/env python3
"""
Redis Caching Test for Distributed Inventory Management System
Tests all Redis caching functionality as specified in the review request
"""

import requests
import time
import json
import subprocess
from datetime import datetime
import sys

# Configuration
BACKEND_URL = "https://stockhub-8.preview.emergentagent.com/api"

class RedisCacheTest:
    def __init__(self):
        self.results = []
        self.session = requests.Session()
        self.session.timeout = 15
        
    def log_result(self, test_name, success, message, details=None):
        """Log test result"""
        result = {
            'test': test_name,
            'success': success,
            'message': message,
            'timestamp': datetime.now().isoformat(),
            'details': details or {}
        }
        self.results.append(result)
        status = "✅ PASS" if success else "❌ FAIL"
        print(f"{status}: {test_name} - {message}")
        if details:
            print(f"   Details: {details}")
    
    def clear_redis_cache(self):
        """Clear Redis cache to ensure clean test state"""
        try:
            result = subprocess.run(['redis-cli', 'FLUSHALL'], 
                                  capture_output=True, text=True, timeout=5)
            if result.returncode == 0:
                print("🧹 Redis cache cleared for clean testing")
                return True
            else:
                print(f"⚠️ Failed to clear Redis cache: {result.stderr}")
                return False
        except Exception as e:
            print(f"⚠️ Error clearing Redis cache: {str(e)}")
            return False
    
    def check_redis_keys(self, pattern="*"):
        """Check what keys exist in Redis"""
        try:
            result = subprocess.run(['redis-cli', 'KEYS', pattern], 
                                  capture_output=True, text=True, timeout=5)
            if result.returncode == 0:
                keys = result.stdout.strip().split('\n') if result.stdout.strip() else []
                return [key for key in keys if key]  # Filter empty strings
            return []
        except Exception as e:
            print(f"⚠️ Error checking Redis keys: {str(e)}")
            return []
    
    def measure_response_time(self, url, method='GET', data=None):
        """Measure response time for API call"""
        start_time = time.time()
        try:
            if method == 'GET':
                response = self.session.get(url)
            elif method == 'POST':
                response = self.session.post(url, json=data)
            end_time = time.time()
            
            response_time = (end_time - start_time) * 1000  # Convert to milliseconds
            return response, response_time
        except Exception as e:
            end_time = time.time()
            response_time = (end_time - start_time) * 1000
            return None, response_time
    
    def test_inventory_caching(self):
        """Test inventory endpoint caching"""
        print("\n📦 Testing Inventory Caching")
        print("-" * 50)
        
        # Clear cache first
        self.clear_redis_cache()
        
        # Test 1: GET /api/inventory (should cache with key 'all')
        url = f"{BACKEND_URL}/inventory"
        
        # First call - should be cache miss
        response1, time1 = self.measure_response_time(url)
        if response1 and response1.status_code == 200:
            self.log_result("Inventory Cache - First Call (Miss)", True, 
                          f"Response time: {time1:.1f}ms", {"response_time": time1})
            
            # Check if cache key was created
            cache_keys = self.check_redis_keys("*inventory*")
            if any("all" in key for key in cache_keys):
                self.log_result("Inventory Cache - Key Creation", True, 
                              f"Cache key created: {cache_keys}")
            else:
                self.log_result("Inventory Cache - Key Creation", False, 
                              f"No inventory cache key found. Keys: {cache_keys}")
            
            # Second call - should be cache hit
            time.sleep(1)  # Small delay
            response2, time2 = self.measure_response_time(url)
            if response2 and response2.status_code == 200:
                # Cache hit should be significantly faster
                if time2 < time1 * 0.8:  # At least 20% faster
                    self.log_result("Inventory Cache - Second Call (Hit)", True, 
                                  f"Cache hit faster: {time2:.1f}ms vs {time1:.1f}ms")
                else:
                    self.log_result("Inventory Cache - Second Call (Hit)", False, 
                                  f"No performance improvement: {time2:.1f}ms vs {time1:.1f}ms")
            else:
                self.log_result("Inventory Cache - Second Call", False, 
                              f"Second call failed: {response2.status_code if response2 else 'No response'}")
        else:
            self.log_result("Inventory Cache - First Call", False, 
                          f"First call failed: {response1.status_code if response1 else 'No response'}")
    
    def test_inventory_store_caching(self):
        """Test inventory by store caching"""
        print("\n🏪 Testing Inventory by Store Caching")
        print("-" * 50)
        
        # Test GET /api/inventory/store/{storeId}
        store_id = 1
        url = f"{BACKEND_URL}/inventory/store/{store_id}"
        
        # First call - cache miss
        response1, time1 = self.measure_response_time(url)
        if response1 and response1.status_code == 200:
            self.log_result("Store Inventory Cache - First Call", True, 
                          f"Store {store_id} inventory loaded: {time1:.1f}ms")
            
            # Check cache key
            cache_keys = self.check_redis_keys("*inventory*")
            store_key_found = any(f"store:{store_id}" in key for key in cache_keys)
            if store_key_found:
                self.log_result("Store Inventory Cache - Key Creation", True, 
                              f"Store cache key created")
            else:
                self.log_result("Store Inventory Cache - Key Creation", False, 
                              f"Store cache key not found. Keys: {cache_keys}")
            
            # Second call - cache hit
            time.sleep(1)
            response2, time2 = self.measure_response_time(url)
            if response2 and response2.status_code == 200:
                if time2 < time1 * 0.8:
                    self.log_result("Store Inventory Cache - Second Call", True, 
                                  f"Cache hit: {time2:.1f}ms vs {time1:.1f}ms")
                else:
                    self.log_result("Store Inventory Cache - Second Call", False, 
                                  f"No cache benefit: {time2:.1f}ms vs {time1:.1f}ms")
        else:
            self.log_result("Store Inventory Cache - First Call", False, 
                          f"Failed: {response1.status_code if response1 else 'No response'}")
    
    def test_inventory_product_caching(self):
        """Test inventory by product caching"""
        print("\n🛍️ Testing Inventory by Product Caching")
        print("-" * 50)
        
        # Test GET /api/inventory/product/{productId}
        product_id = 1
        url = f"{BACKEND_URL}/inventory/product/{product_id}"
        
        # First call - cache miss
        response1, time1 = self.measure_response_time(url)
        if response1 and response1.status_code == 200:
            self.log_result("Product Inventory Cache - First Call", True, 
                          f"Product {product_id} inventory loaded: {time1:.1f}ms")
            
            # Second call - cache hit
            time.sleep(1)
            response2, time2 = self.measure_response_time(url)
            if response2 and response2.status_code == 200:
                if time2 < time1 * 0.8:
                    self.log_result("Product Inventory Cache - Second Call", True, 
                                  f"Cache hit: {time2:.1f}ms vs {time1:.1f}ms")
                else:
                    self.log_result("Product Inventory Cache - Second Call", False, 
                                  f"No cache benefit: {time2:.1f}ms vs {time1:.1f}ms")
        else:
            self.log_result("Product Inventory Cache - First Call", False, 
                          f"Failed: {response1.status_code if response1 else 'No response'}")
    
    def test_low_stock_caching(self):
        """Test low stock caching with 2-minute TTL"""
        print("\n⚠️ Testing Low Stock Caching")
        print("-" * 50)
        
        # Test GET /api/inventory/low-stock
        url = f"{BACKEND_URL}/inventory/low-stock"
        
        # First call - cache miss
        response1, time1 = self.measure_response_time(url)
        if response1 and response1.status_code == 200:
            self.log_result("Low Stock Cache - First Call", True, 
                          f"Low stock items loaded: {time1:.1f}ms")
            
            # Check cache key
            cache_keys = self.check_redis_keys("*low-stock*")
            if cache_keys:
                self.log_result("Low Stock Cache - Key Creation", True, 
                              f"Low stock cache key created: {cache_keys}")
            else:
                self.log_result("Low Stock Cache - Key Creation", False, 
                              "No low stock cache key found")
            
            # Second call - cache hit
            time.sleep(1)
            response2, time2 = self.measure_response_time(url)
            if response2 and response2.status_code == 200:
                if time2 < time1 * 0.8:
                    self.log_result("Low Stock Cache - Second Call", True, 
                                  f"Cache hit: {time2:.1f}ms vs {time1:.1f}ms")
                else:
                    self.log_result("Low Stock Cache - Second Call", False, 
                                  f"No cache benefit: {time2:.1f}ms vs {time1:.1f}ms")
        else:
            self.log_result("Low Stock Cache - First Call", False, 
                          f"Failed: {response1.status_code if response1 else 'No response'}")
    
    def test_product_caching(self):
        """Test product caching with 30-minute TTL"""
        print("\n🛍️ Testing Product Caching")
        print("-" * 50)
        
        # Test GET /api/products (should cache all products for 30 minutes)
        url = f"{BACKEND_URL}/products"
        
        # First call - cache miss
        response1, time1 = self.measure_response_time(url)
        if response1 and response1.status_code == 200:
            self.log_result("Products Cache - All Products First Call", True, 
                          f"All products loaded: {time1:.1f}ms")
            
            # Second call - cache hit
            time.sleep(1)
            response2, time2 = self.measure_response_time(url)
            if response2 and response2.status_code == 200:
                if time2 < time1 * 0.8:
                    self.log_result("Products Cache - All Products Second Call", True, 
                                  f"Cache hit: {time2:.1f}ms vs {time1:.1f}ms")
                else:
                    self.log_result("Products Cache - All Products Second Call", False, 
                                  f"No cache benefit: {time2:.1f}ms vs {time1:.1f}ms")
        else:
            self.log_result("Products Cache - All Products", False, 
                          f"Failed: {response1.status_code if response1 else 'No response'}")
        
        # Test GET /api/products/{id} (should cache individual products)
        product_id = 1
        url = f"{BACKEND_URL}/products/{product_id}"
        
        response1, time1 = self.measure_response_time(url)
        if response1 and response1.status_code == 200:
            self.log_result("Products Cache - Individual Product First Call", True, 
                          f"Product {product_id} loaded: {time1:.1f}ms")
            
            time.sleep(1)
            response2, time2 = self.measure_response_time(url)
            if response2 and response2.status_code == 200:
                if time2 < time1 * 0.8:
                    self.log_result("Products Cache - Individual Product Second Call", True, 
                                  f"Cache hit: {time2:.1f}ms vs {time1:.1f}ms")
                else:
                    self.log_result("Products Cache - Individual Product Second Call", False, 
                                  f"No cache benefit: {time2:.1f}ms vs {time1:.1f}ms")
        
        # Test GET /api/products/categories (should cache categories)
        url = f"{BACKEND_URL}/products/categories"
        
        response1, time1 = self.measure_response_time(url)
        if response1 and response1.status_code == 200:
            self.log_result("Products Cache - Categories First Call", True, 
                          f"Categories loaded: {time1:.1f}ms")
            
            time.sleep(1)
            response2, time2 = self.measure_response_time(url)
            if response2 and response2.status_code == 200:
                if time2 < time1 * 0.8:
                    self.log_result("Products Cache - Categories Second Call", True, 
                                  f"Cache hit: {time2:.1f}ms vs {time1:.1f}ms")
                else:
                    self.log_result("Products Cache - Categories Second Call", False, 
                                  f"No cache benefit: {time2:.1f}ms vs {time1:.1f}ms")
    
    def test_store_caching(self):
        """Test store caching with 1-hour TTL"""
        print("\n🏪 Testing Store Caching")
        print("-" * 50)
        
        # Test GET /api/stores (should cache all stores for 1 hour)
        url = f"{BACKEND_URL}/stores"
        
        # First call - cache miss
        response1, time1 = self.measure_response_time(url)
        if response1 and response1.status_code == 200:
            self.log_result("Stores Cache - All Stores First Call", True, 
                          f"All stores loaded: {time1:.1f}ms")
            
            # Second call - cache hit
            time.sleep(1)
            response2, time2 = self.measure_response_time(url)
            if response2 and response2.status_code == 200:
                if time2 < time1 * 0.8:
                    self.log_result("Stores Cache - All Stores Second Call", True, 
                                  f"Cache hit: {time2:.1f}ms vs {time1:.1f}ms")
                else:
                    self.log_result("Stores Cache - All Stores Second Call", False, 
                                  f"No cache benefit: {time2:.1f}ms vs {time1:.1f}ms")
        else:
            self.log_result("Stores Cache - All Stores", False, 
                          f"Failed: {response1.status_code if response1 else 'No response'}")
        
        # Test GET /api/stores/{id} (should cache individual stores)
        store_id = 1
        url = f"{BACKEND_URL}/stores/{store_id}"
        
        response1, time1 = self.measure_response_time(url)
        if response1 and response1.status_code == 200:
            self.log_result("Stores Cache - Individual Store First Call", True, 
                          f"Store {store_id} loaded: {time1:.1f}ms")
            
            time.sleep(1)
            response2, time2 = self.measure_response_time(url)
            if response2 and response2.status_code == 200:
                if time2 < time1 * 0.8:
                    self.log_result("Stores Cache - Individual Store Second Call", True, 
                                  f"Cache hit: {time2:.1f}ms vs {time1:.1f}ms")
                else:
                    self.log_result("Stores Cache - Individual Store Second Call", False, 
                                  f"No cache benefit: {time2:.1f}ms vs {time1:.1f}ms")
    
    def test_cache_eviction_stock_in(self):
        """Test cache eviction after stock-in operations"""
        print("\n🔄 Testing Cache Eviction - Stock In")
        print("-" * 50)
        
        # First, populate cache by calling inventory endpoints
        inventory_url = f"{BACKEND_URL}/inventory"
        low_stock_url = f"{BACKEND_URL}/inventory/low-stock"
        
        # Populate cache
        self.session.get(inventory_url)
        self.session.get(low_stock_url)
        
        # Check cache keys exist
        cache_keys_before = self.check_redis_keys("*inventory*")
        if cache_keys_before:
            self.log_result("Cache Eviction - Cache Populated", True, 
                          f"Cache keys before stock-in: {len(cache_keys_before)}")
            
            # Perform stock-in operation (should evict cache)
            stock_in_data = {
                "storeId": 1,
                "productId": 1,
                "quantity": 10,
                "referenceId": "CACHE-TEST-001",
                "notes": "Testing cache eviction"
            }
            
            stock_in_url = f"{BACKEND_URL}/inventory/stock-in"
            response = self.session.post(stock_in_url, json=stock_in_data)
            
            if response.status_code == 200:
                self.log_result("Cache Eviction - Stock In Operation", True, 
                              "Stock-in operation successful")
                
                # Wait a moment for cache eviction
                time.sleep(2)
                
                # Check if cache was evicted
                cache_keys_after = self.check_redis_keys("*inventory*")
                if len(cache_keys_after) < len(cache_keys_before):
                    self.log_result("Cache Eviction - Inventory Cache Cleared", True, 
                                  f"Cache keys after stock-in: {len(cache_keys_after)}")
                else:
                    self.log_result("Cache Eviction - Inventory Cache Cleared", False, 
                                  f"Cache not evicted. Before: {len(cache_keys_before)}, After: {len(cache_keys_after)}")
            else:
                self.log_result("Cache Eviction - Stock In Operation", False, 
                              f"Stock-in failed: {response.status_code}")
        else:
            self.log_result("Cache Eviction - Cache Populated", False, 
                          "No cache keys found to test eviction")
    
    def test_cache_eviction_stock_out(self):
        """Test cache eviction after stock-out operations"""
        print("\n🔄 Testing Cache Eviction - Stock Out")
        print("-" * 50)
        
        # Populate cache first
        inventory_url = f"{BACKEND_URL}/inventory"
        self.session.get(inventory_url)
        
        cache_keys_before = self.check_redis_keys("*inventory*")
        if cache_keys_before:
            # Perform stock-out operation
            stock_out_data = {
                "storeId": 1,
                "productId": 1,
                "quantity": 5,
                "referenceId": "CACHE-TEST-002",
                "notes": "Testing cache eviction on stock out"
            }
            
            stock_out_url = f"{BACKEND_URL}/inventory/stock-out"
            response = self.session.post(stock_out_url, json=stock_out_data)
            
            if response.status_code == 200:
                self.log_result("Cache Eviction - Stock Out Operation", True, 
                              "Stock-out operation successful")
                
                time.sleep(2)
                
                # Check cache eviction
                cache_keys_after = self.check_redis_keys("*inventory*")
                if len(cache_keys_after) < len(cache_keys_before):
                    self.log_result("Cache Eviction - Stock Out Cache Cleared", True, 
                                  f"Cache evicted after stock-out")
                else:
                    self.log_result("Cache Eviction - Stock Out Cache Cleared", False, 
                                  f"Cache not evicted after stock-out")
            else:
                self.log_result("Cache Eviction - Stock Out Operation", False, 
                              f"Stock-out failed: {response.status_code}")
    
    def test_redis_connectivity(self):
        """Test Redis connectivity and basic operations"""
        print("\n🔗 Testing Redis Connectivity")
        print("-" * 50)
        
        try:
            # Test Redis ping
            result = subprocess.run(['redis-cli', 'ping'], 
                                  capture_output=True, text=True, timeout=5)
            if result.returncode == 0 and 'PONG' in result.stdout:
                self.log_result("Redis Connectivity - Ping", True, "Redis responding to ping")
                
                # Test Redis info
                info_result = subprocess.run(['redis-cli', 'info', 'server'], 
                                           capture_output=True, text=True, timeout=5)
                if info_result.returncode == 0:
                    redis_version = "Unknown"
                    for line in info_result.stdout.split('\n'):
                        if line.startswith('redis_version:'):
                            redis_version = line.split(':')[1].strip()
                            break
                    
                    self.log_result("Redis Connectivity - Info", True, 
                                  f"Redis version: {redis_version}")
                else:
                    self.log_result("Redis Connectivity - Info", False, 
                                  "Could not get Redis info")
            else:
                self.log_result("Redis Connectivity - Ping", False, 
                              f"Redis not responding: {result.stderr}")
        except Exception as e:
            self.log_result("Redis Connectivity", False, f"Redis test error: {str(e)}")
    
    def test_performance_comparison(self):
        """Test performance comparison between cache miss and cache hit"""
        print("\n⚡ Testing Performance Comparison")
        print("-" * 50)
        
        # Clear cache for clean test
        self.clear_redis_cache()
        
        # Test multiple endpoints for performance
        endpoints = [
            ("/inventory", "All Inventory"),
            ("/products", "All Products"),
            ("/stores", "All Stores"),
            ("/inventory/low-stock", "Low Stock Items")
        ]
        
        for endpoint, name in endpoints:
            url = f"{BACKEND_URL}{endpoint}"
            
            # First call - cache miss
            response1, time1 = self.measure_response_time(url)
            if response1 and response1.status_code == 200:
                # Second call - cache hit
                time.sleep(0.5)
                response2, time2 = self.measure_response_time(url)
                
                if response2 and response2.status_code == 200:
                    improvement = ((time1 - time2) / time1) * 100
                    if improvement > 20:  # At least 20% improvement
                        self.log_result(f"Performance - {name}", True, 
                                      f"Cache improved performance by {improvement:.1f}% ({time1:.1f}ms → {time2:.1f}ms)")
                    else:
                        self.log_result(f"Performance - {name}", False, 
                                      f"Minimal performance improvement: {improvement:.1f}% ({time1:.1f}ms → {time2:.1f}ms)")
                else:
                    self.log_result(f"Performance - {name}", False, 
                                  "Second call failed")
            else:
                self.log_result(f"Performance - {name}", False, 
                              "First call failed")
    
    def run_all_tests(self):
        """Run all Redis caching tests"""
        print("🚀 Starting Redis Caching Tests for Distributed Inventory Management System")
        print("=" * 80)
        
        # Basic connectivity test
        self.test_redis_connectivity()
        
        # Core caching tests
        self.test_inventory_caching()
        self.test_inventory_store_caching()
        self.test_inventory_product_caching()
        self.test_low_stock_caching()
        self.test_product_caching()
        self.test_store_caching()
        
        # Cache eviction tests
        self.test_cache_eviction_stock_in()
        self.test_cache_eviction_stock_out()
        
        # Performance tests
        self.test_performance_comparison()
        
        # Summary
        print("\n" + "=" * 80)
        print("📊 REDIS CACHING TEST SUMMARY")
        print("=" * 80)
        
        passed = sum(1 for r in self.results if r['success'])
        failed = len(self.results) - passed
        
        print(f"Total Tests: {len(self.results)}")
        print(f"✅ Passed: {passed}")
        print(f"❌ Failed: {failed}")
        print(f"Success Rate: {(passed/len(self.results)*100):.1f}%")
        
        if failed > 0:
            print("\n🔍 FAILED TESTS:")
            for result in self.results:
                if not result['success']:
                    print(f"   • {result['test']}: {result['message']}")
        
        return self.results

if __name__ == "__main__":
    tester = RedisCacheTest()
    results = tester.run_all_tests()
    
    # Exit with error code if any critical tests failed
    critical_failures = [r for r in results if not r['success'] and 
                        any(keyword in r['test'].lower() for keyword in 
                            ['connectivity', 'cache', 'performance'])]
    
    if critical_failures:
        print(f"\n⚠️  {len(critical_failures)} critical Redis caching test(s) failed!")
        sys.exit(1)
    else:
        print("\n🎉 All Redis caching tests passed!")
        sys.exit(0)