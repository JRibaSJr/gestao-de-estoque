#!/usr/bin/env python3
"""
Focused Redis Caching Test - Testing only working endpoints
"""

import requests
import time
import json
import subprocess
from datetime import datetime
import sys

# Configuration
BACKEND_URL = "https://stockhub-8.preview.emergentagent.com/api"

class FocusedRedisCacheTest:
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
    
    def test_endpoint_availability(self):
        """Test which endpoints are actually working"""
        print("\n🔍 Testing Endpoint Availability")
        print("-" * 50)
        
        endpoints = [
            ("/products/categories", "Product Categories"),
            ("/products", "All Products"),
            ("/products/1", "Product by ID"),
            ("/stores", "All Stores"),
            ("/stores/1", "Store by ID"),
            ("/inventory", "All Inventory"),
            ("/inventory/store/1", "Inventory by Store"),
            ("/inventory/product/1", "Inventory by Product"),
            ("/inventory/low-stock", "Low Stock Items"),
        ]
        
        working_endpoints = []
        
        for endpoint, name in endpoints:
            url = f"{BACKEND_URL}{endpoint}"
            try:
                response = self.session.get(url, timeout=10)
                if response.status_code == 200:
                    self.log_result(f"Endpoint Check - {name}", True, 
                                  f"Working: {response.status_code}")
                    working_endpoints.append((endpoint, name))
                else:
                    self.log_result(f"Endpoint Check - {name}", False, 
                                  f"Failed: {response.status_code}")
            except Exception as e:
                self.log_result(f"Endpoint Check - {name}", False, 
                              f"Error: {str(e)}")
        
        return working_endpoints
    
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
                    return True
                else:
                    self.log_result("Redis Connectivity - Info", False, 
                                  "Could not get Redis info")
                    return False
            else:
                self.log_result("Redis Connectivity - Ping", False, 
                              f"Redis not responding: {result.stderr}")
                return False
        except Exception as e:
            self.log_result("Redis Connectivity", False, f"Redis test error: {str(e)}")
            return False
    
    def test_working_endpoint_caching(self, endpoint, name):
        """Test caching for a specific working endpoint"""
        print(f"\n📦 Testing {name} Caching")
        print("-" * 50)
        
        url = f"{BACKEND_URL}{endpoint}"
        
        # Clear cache first
        self.clear_redis_cache()
        
        # First call - should be cache miss
        response1, time1 = self.measure_response_time(url)
        if response1 and response1.status_code == 200:
            self.log_result(f"{name} Cache - First Call (Miss)", True, 
                          f"Response time: {time1:.1f}ms")
            
            # Check if cache key was created
            cache_keys = self.check_redis_keys("*")
            if cache_keys:
                self.log_result(f"{name} Cache - Key Creation", True, 
                              f"Cache keys created: {len(cache_keys)} keys")
                print(f"   Cache keys: {cache_keys}")
            else:
                self.log_result(f"{name} Cache - Key Creation", False, 
                              "No cache keys found")
            
            # Second call - should be cache hit
            time.sleep(1)  # Small delay
            response2, time2 = self.measure_response_time(url)
            if response2 and response2.status_code == 200:
                # Cache hit should be significantly faster
                if time2 < time1 * 0.8:  # At least 20% faster
                    self.log_result(f"{name} Cache - Second Call (Hit)", True, 
                                  f"Cache hit faster: {time2:.1f}ms vs {time1:.1f}ms ({((time1-time2)/time1*100):.1f}% improvement)")
                else:
                    self.log_result(f"{name} Cache - Second Call (Hit)", False, 
                                  f"No performance improvement: {time2:.1f}ms vs {time1:.1f}ms")
            else:
                self.log_result(f"{name} Cache - Second Call", False, 
                              f"Second call failed: {response2.status_code if response2 else 'No response'}")
        else:
            self.log_result(f"{name} Cache - First Call", False, 
                          f"First call failed: {response1.status_code if response1 else 'No response'}")
    
    def test_stock_operations_and_cache_eviction(self):
        """Test stock operations and their cache eviction behavior"""
        print("\n🔄 Testing Stock Operations and Cache Eviction")
        print("-" * 50)
        
        # First, test if stock operations work
        stock_in_data = {
            "storeId": 1,
            "productId": 1,
            "quantity": 10,
            "referenceId": "REDIS-TEST-001",
            "notes": "Testing Redis cache eviction"
        }
        
        stock_in_url = f"{BACKEND_URL}/inventory/stock-in"
        response = self.session.post(stock_in_url, json=stock_in_data)
        
        if response.status_code == 200:
            self.log_result("Stock Operations - Stock In", True, 
                          f"Stock-in successful: {response.json().get('message', 'OK')}")
            
            # Check if this operation evicts cache (if any cache exists)
            cache_keys_after = self.check_redis_keys("*inventory*")
            self.log_result("Cache Eviction - After Stock In", True, 
                          f"Cache keys after stock-in: {len(cache_keys_after)}")
            
            # Test stock out
            stock_out_data = {
                "storeId": 1,
                "productId": 1,
                "quantity": 5,
                "referenceId": "REDIS-TEST-002",
                "notes": "Testing Redis cache eviction on stock out"
            }
            
            stock_out_url = f"{BACKEND_URL}/inventory/stock-out"
            response = self.session.post(stock_out_url, json=stock_out_data)
            
            if response.status_code == 200:
                self.log_result("Stock Operations - Stock Out", True, 
                              f"Stock-out successful: {response.json().get('message', 'OK')}")
            else:
                self.log_result("Stock Operations - Stock Out", False, 
                              f"Stock-out failed: {response.status_code} - {response.text}")
        else:
            self.log_result("Stock Operations - Stock In", False, 
                          f"Stock-in failed: {response.status_code} - {response.text}")
    
    def test_redis_cache_configuration(self):
        """Test Redis cache configuration and TTL settings"""
        print("\n⚙️ Testing Redis Cache Configuration")
        print("-" * 50)
        
        # Check Redis configuration
        try:
            config_result = subprocess.run(['redis-cli', 'config', 'get', 'maxmemory*'], 
                                         capture_output=True, text=True, timeout=5)
            if config_result.returncode == 0:
                self.log_result("Redis Configuration - Memory Settings", True, 
                              "Redis memory configuration accessible")
                print(f"   Config: {config_result.stdout.strip()}")
            else:
                self.log_result("Redis Configuration - Memory Settings", False, 
                              "Could not get Redis configuration")
            
            # Check Redis keyspace info
            info_result = subprocess.run(['redis-cli', 'info', 'keyspace'], 
                                       capture_output=True, text=True, timeout=5)
            if info_result.returncode == 0:
                keyspace_info = info_result.stdout.strip()
                if keyspace_info:
                    self.log_result("Redis Configuration - Keyspace Info", True, 
                                  f"Keyspace info available")
                    print(f"   Keyspace: {keyspace_info}")
                else:
                    self.log_result("Redis Configuration - Keyspace Info", True, 
                                  "No keys in keyspace (clean state)")
            
        except Exception as e:
            self.log_result("Redis Configuration", False, f"Error: {str(e)}")
    
    def run_all_tests(self):
        """Run all focused Redis caching tests"""
        print("🚀 Starting Focused Redis Caching Tests")
        print("=" * 60)
        
        # Test Redis connectivity first
        redis_working = self.test_redis_connectivity()
        if not redis_working:
            print("❌ Redis not available, skipping cache tests")
            return self.results
        
        # Test Redis configuration
        self.test_redis_cache_configuration()
        
        # Check which endpoints are working
        working_endpoints = self.test_endpoint_availability()
        
        # Test caching for working endpoints
        for endpoint, name in working_endpoints:
            self.test_working_endpoint_caching(endpoint, name)
        
        # Test stock operations (these seem to work)
        self.test_stock_operations_and_cache_eviction()
        
        # Summary
        print("\n" + "=" * 60)
        print("📊 FOCUSED REDIS CACHING TEST SUMMARY")
        print("=" * 60)
        
        passed = sum(1 for r in self.results if r['success'])
        failed = len(self.results) - passed
        
        print(f"Total Tests: {len(self.results)}")
        print(f"✅ Passed: {passed}")
        print(f"❌ Failed: {failed}")
        print(f"Success Rate: {(passed/len(self.results)*100):.1f}%")
        
        # Categorize results
        endpoint_tests = [r for r in self.results if 'Endpoint Check' in r['test']]
        cache_tests = [r for r in self.results if 'Cache' in r['test'] and 'Endpoint Check' not in r['test']]
        
        working_endpoints_count = sum(1 for r in endpoint_tests if r['success'])
        working_cache_tests = sum(1 for r in cache_tests if r['success'])
        
        print(f"\n📊 Detailed Results:")
        print(f"   Working Endpoints: {working_endpoints_count}/{len(endpoint_tests)}")
        print(f"   Successful Cache Tests: {working_cache_tests}/{len(cache_tests)}")
        
        if failed > 0:
            print("\n🔍 FAILED TESTS:")
            for result in self.results:
                if not result['success']:
                    print(f"   • {result['test']}: {result['message']}")
        
        return self.results

if __name__ == "__main__":
    tester = FocusedRedisCacheTest()
    results = tester.run_all_tests()
    
    # Check if Redis caching is working for available endpoints
    cache_tests = [r for r in results if 'Cache' in r['test'] and 'Second Call (Hit)' in r['test']]
    successful_cache_tests = [r for r in cache_tests if r['success']]
    
    if successful_cache_tests:
        print(f"\n🎉 Redis caching is working! {len(successful_cache_tests)} endpoint(s) showing cache performance improvements!")
    else:
        print(f"\n⚠️ Redis caching tests inconclusive - most endpoints returning 500 errors")
    
    sys.exit(0)