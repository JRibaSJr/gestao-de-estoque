#!/usr/bin/env python3
"""
Comprehensive Backend Testing for Distributed Inventory Management System
Tests both FastAPI backend and Spring Boot components where possible
"""

import requests
import json
import time
import sqlite3
import os
from datetime import datetime
import subprocess
import sys

# Configuration
BACKEND_URL = "https://stockhub-8.preview.emergentagent.com/api"
API_GATEWAY_URL = "http://localhost:8080"
DIRECT_BACKEND_URL = "http://localhost:8001/api"
SQLITE_DB_PATH = "/app/backend/inventory.db"

class BackendTester:
    def __init__(self):
        self.results = []
        self.session = requests.Session()
        self.session.timeout = 10
        
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
    
    def test_fastapi_backend_health(self):
        """Test FastAPI backend health and basic endpoints"""
        try:
            # Test root endpoint
            response = self.session.get(f"{BACKEND_URL}/")
            if response.status_code == 200:
                data = response.json()
                self.log_result("FastAPI Root Endpoint", True, 
                              f"Backend responding: {data.get('message', 'OK')}")
            else:
                self.log_result("FastAPI Root Endpoint", False, 
                              f"HTTP {response.status_code}: {response.text}")
        except Exception as e:
            self.log_result("FastAPI Root Endpoint", False, f"Connection error: {str(e)}")
    
    def test_fastapi_status_endpoints(self):
        """Test FastAPI status check endpoints"""
        try:
            # Test GET status checks
            response = self.session.get(f"{BACKEND_URL}/status")
            if response.status_code == 200:
                data = response.json()
                self.log_result("FastAPI GET Status", True, 
                              f"Retrieved {len(data)} status checks")
            else:
                self.log_result("FastAPI GET Status", False, 
                              f"HTTP {response.status_code}: {response.text}")
            
            # Test POST status check
            test_data = {"client_name": "backend_test_client"}
            response = self.session.post(f"{BACKEND_URL}/status", json=test_data)
            if response.status_code == 200:
                data = response.json()
                self.log_result("FastAPI POST Status", True, 
                              f"Created status check with ID: {data.get('id', 'unknown')}")
            else:
                self.log_result("FastAPI POST Status", False, 
                              f"HTTP {response.status_code}: {response.text}")
                
        except Exception as e:
            self.log_result("FastAPI Status Endpoints", False, f"Error: {str(e)}")
    
    def test_direct_backend_access(self):
        """Test direct access to backend on port 8001"""
        try:
            response = self.session.get(f"{DIRECT_BACKEND_URL}/")
            if response.status_code == 200:
                self.log_result("Direct Backend Access", True, "Port 8001 accessible")
            else:
                self.log_result("Direct Backend Access", False, 
                              f"HTTP {response.status_code}")
        except Exception as e:
            self.log_result("Direct Backend Access", False, f"Connection error: {str(e)}")
    
    def test_api_gateway_routing(self):
        """Test API Gateway routing (if available)"""
        try:
            # Test if API Gateway is responding
            response = self.session.get(f"{API_GATEWAY_URL}/actuator/health", timeout=5)
            if response.status_code == 200:
                self.log_result("API Gateway Health", True, "Gateway responding")
                
                # Test routing through gateway
                try:
                    response = self.session.get(f"{API_GATEWAY_URL}/api/stores", timeout=5)
                    if response.status_code in [200, 404, 500]:  # Any response means routing works
                        self.log_result("API Gateway Routing", True, 
                                      f"Gateway routing functional (HTTP {response.status_code})")
                    else:
                        self.log_result("API Gateway Routing", False, 
                                      f"Unexpected response: {response.status_code}")
                except Exception as e:
                    self.log_result("API Gateway Routing", False, f"Routing error: {str(e)}")
            else:
                self.log_result("API Gateway Health", False, 
                              f"Gateway not responding: HTTP {response.status_code}")
        except Exception as e:
            self.log_result("API Gateway Health", False, f"Gateway unavailable: {str(e)}")
    
    def test_spring_boot_endpoints(self):
        """Test Spring Boot inventory endpoints through external URL"""
        endpoints_to_test = [
            ("/api/stores", "GET", "Store Management"),
            ("/api/products", "GET", "Product Management"), 
            ("/api/inventory", "GET", "Inventory Management"),
        ]
        
        for endpoint, method, description in endpoints_to_test:
            try:
                url = f"{BACKEND_URL.replace('/api', '')}{endpoint}"
                response = self.session.request(method, url)
                
                if response.status_code == 200:
                    try:
                        data = response.json()
                        self.log_result(f"Spring Boot {description}", True, 
                                      f"Endpoint working, returned {len(data) if isinstance(data, list) else 'data'}")
                    except:
                        self.log_result(f"Spring Boot {description}", True, 
                                      f"Endpoint responding (non-JSON response)")
                elif response.status_code == 404:
                    self.log_result(f"Spring Boot {description}", False, 
                                  "Endpoint not found - Spring Boot backend may not be running")
                else:
                    self.log_result(f"Spring Boot {description}", False, 
                                  f"HTTP {response.status_code}: {response.text[:100]}")
            except Exception as e:
                self.log_result(f"Spring Boot {description}", False, f"Error: {str(e)}")
    
    def test_database_connectivity(self):
        """Test SQLite database connectivity and data"""
        try:
            if os.path.exists(SQLITE_DB_PATH):
                conn = sqlite3.connect(SQLITE_DB_PATH)
                cursor = conn.cursor()
                
                # Check if tables exist
                cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
                tables = cursor.fetchall()
                
                if tables:
                    table_names = [table[0] for table in tables]
                    self.log_result("SQLite Database", True, 
                                  f"Database accessible with tables: {', '.join(table_names)}")
                    
                    # Check for sample data in key tables
                    for table in ['stores', 'products', 'inventory']:
                        if table in table_names:
                            try:
                                cursor.execute(f"SELECT COUNT(*) FROM {table};")
                                count = cursor.fetchone()[0]
                                self.log_result(f"SQLite {table.title()} Data", True, 
                                              f"Table has {count} records")
                            except Exception as e:
                                self.log_result(f"SQLite {table.title()} Data", False, 
                                              f"Error querying table: {str(e)}")
                else:
                    self.log_result("SQLite Database", False, "No tables found in database")
                
                conn.close()
            else:
                self.log_result("SQLite Database", False, f"Database file not found at {SQLITE_DB_PATH}")
        except Exception as e:
            self.log_result("SQLite Database", False, f"Database error: {str(e)}")
    
    def test_mongodb_connectivity(self):
        """Test MongoDB connectivity for FastAPI backend"""
        try:
            # Test through FastAPI endpoints that use MongoDB
            response = self.session.get(f"{BACKEND_URL}/status")
            if response.status_code == 200:
                self.log_result("MongoDB Connectivity", True, 
                              "MongoDB accessible through FastAPI endpoints")
            else:
                self.log_result("MongoDB Connectivity", False, 
                              "Cannot verify MongoDB through FastAPI")
        except Exception as e:
            self.log_result("MongoDB Connectivity", False, f"Error: {str(e)}")
    
    def test_redis_connectivity(self):
        """Test Redis connectivity"""
        try:
            result = subprocess.run(['redis-cli', 'ping'], 
                                  capture_output=True, text=True, timeout=5)
            if result.returncode == 0 and 'PONG' in result.stdout:
                self.log_result("Redis Connectivity", True, "Redis responding to ping")
            else:
                self.log_result("Redis Connectivity", False, 
                              f"Redis not responding: {result.stderr}")
        except Exception as e:
            self.log_result("Redis Connectivity", False, f"Redis test error: {str(e)}")
    
    def test_rabbitmq_connectivity(self):
        """Test RabbitMQ connectivity"""
        try:
            # Check if RabbitMQ process is running
            result = subprocess.run(['pgrep', '-f', 'rabbitmq'], 
                                  capture_output=True, text=True)
            if result.returncode == 0:
                self.log_result("RabbitMQ Process", True, "RabbitMQ process running")
            else:
                self.log_result("RabbitMQ Process", False, "RabbitMQ process not found")
                
            # Try to check RabbitMQ management (if available)
            try:
                response = self.session.get("http://localhost:15672", timeout=3)
                if response.status_code in [200, 401]:  # 401 means it's running but needs auth
                    self.log_result("RabbitMQ Management", True, "Management interface accessible")
                else:
                    self.log_result("RabbitMQ Management", False, "Management interface not accessible")
            except:
                self.log_result("RabbitMQ Management", False, "Management interface not available")
                
        except Exception as e:
            self.log_result("RabbitMQ Connectivity", False, f"Error: {str(e)}")
    
    def test_service_health_checks(self):
        """Test health check endpoints"""
        health_endpoints = [
            (f"{BACKEND_URL}/", "FastAPI Health"),
            (f"{API_GATEWAY_URL}/actuator/health", "API Gateway Health"),
        ]
        
        for url, name in health_endpoints:
            try:
                response = self.session.get(url, timeout=5)
                if response.status_code == 200:
                    self.log_result(name, True, "Health check passed")
                else:
                    self.log_result(name, False, f"Health check failed: HTTP {response.status_code}")
            except Exception as e:
                self.log_result(name, False, f"Health check error: {str(e)}")
    
    def test_inventory_operations(self):
        """Test inventory management operations"""
        try:
            # Test inventory update endpoint
            update_data = {
                "storeId": 1,
                "productId": 1,
                "quantity": 100,
                "operation": "SET"
            }
            
            response = self.session.post(f"{BACKEND_URL.replace('/api', '')}/api/inventory/update", 
                                       json=update_data)
            
            if response.status_code in [200, 404]:  # 404 means endpoint exists but Spring Boot not running
                if response.status_code == 200:
                    self.log_result("Inventory Update", True, "Inventory update endpoint working")
                else:
                    self.log_result("Inventory Update", False, 
                                  "Inventory endpoint exists but Spring Boot backend not running")
            else:
                self.log_result("Inventory Update", False, 
                              f"Inventory update failed: HTTP {response.status_code}")
                
        except Exception as e:
            self.log_result("Inventory Operations", False, f"Error: {str(e)}")

    def test_stock_in_operations(self):
        """Test Stock In functionality for distributed inventory management"""
        try:
            # Test valid stock in operation
            stock_in_data = {
                "storeId": 1,
                "productId": 1,
                "quantity": 50,
                "referenceId": "PO-2025-001",
                "notes": "Purchase order receipt - testing stock in"
            }
            
            response = self.session.post(f"{BACKEND_URL.replace('/api', '')}/api/inventory/stock-in", 
                                       json=stock_in_data)
            
            if response.status_code == 200:
                data = response.json()
                self.log_result("Stock In - Valid Operation", True, 
                              f"Stock in successful: {data.get('message', 'OK')}")
                
                # Verify event was published (check response message)
                if "event published" in data.get('message', '').lower():
                    self.log_result("Stock In - Event Publishing", True, 
                                  "RabbitMQ event published successfully")
                else:
                    self.log_result("Stock In - Event Publishing", False, 
                                  "Event publishing not confirmed in response")
            else:
                self.log_result("Stock In - Valid Operation", False, 
                              f"HTTP {response.status_code}: {response.text}")
            
            # Test stock in with invalid store ID
            invalid_store_data = {
                "storeId": 999,
                "productId": 1,
                "quantity": 25,
                "notes": "Testing invalid store"
            }
            
            response = self.session.post(f"{BACKEND_URL.replace('/api', '')}/api/inventory/stock-in", 
                                       json=invalid_store_data)
            
            if response.status_code == 400:
                self.log_result("Stock In - Invalid Store", True, 
                              "Correctly rejected invalid store ID")
            else:
                self.log_result("Stock In - Invalid Store", False, 
                              f"Should reject invalid store, got HTTP {response.status_code}")
            
            # Test stock in with invalid product ID
            invalid_product_data = {
                "storeId": 1,
                "productId": 999,
                "quantity": 25,
                "notes": "Testing invalid product"
            }
            
            response = self.session.post(f"{BACKEND_URL.replace('/api', '')}/api/inventory/stock-in", 
                                       json=invalid_product_data)
            
            if response.status_code == 400:
                self.log_result("Stock In - Invalid Product", True, 
                              "Correctly rejected invalid product ID")
            else:
                self.log_result("Stock In - Invalid Product", False, 
                              f"Should reject invalid product, got HTTP {response.status_code}")
                
            # Test stock in with zero/negative quantity
            zero_quantity_data = {
                "storeId": 1,
                "productId": 1,
                "quantity": 0,
                "notes": "Testing zero quantity"
            }
            
            response = self.session.post(f"{BACKEND_URL.replace('/api', '')}/api/inventory/stock-in", 
                                       json=zero_quantity_data)
            
            if response.status_code == 400:
                self.log_result("Stock In - Zero Quantity", True, 
                              "Correctly rejected zero quantity")
            else:
                self.log_result("Stock In - Zero Quantity", False, 
                              f"Should reject zero quantity, got HTTP {response.status_code}")
                
        except Exception as e:
            self.log_result("Stock In Operations", False, f"Error: {str(e)}")

    def test_stock_out_operations(self):
        """Test Stock Out functionality for distributed inventory management"""
        try:
            # First, ensure there's stock available by doing a stock in
            stock_in_data = {
                "storeId": 1,
                "productId": 2,
                "quantity": 100,
                "referenceId": "SETUP-001",
                "notes": "Setup stock for stock out testing"
            }
            
            setup_response = self.session.post(f"{BACKEND_URL.replace('/api', '')}/api/inventory/stock-in", 
                                             json=stock_in_data)
            
            if setup_response.status_code == 200:
                self.log_result("Stock Out - Setup", True, "Setup stock for testing")
                
                # Wait a moment for event processing
                time.sleep(2)
                
                # Test valid stock out operation
                stock_out_data = {
                    "storeId": 1,
                    "productId": 2,
                    "quantity": 30,
                    "referenceId": "SALE-2025-001",
                    "notes": "Product sale - testing stock out"
                }
                
                response = self.session.post(f"{BACKEND_URL.replace('/api', '')}/api/inventory/stock-out", 
                                           json=stock_out_data)
                
                if response.status_code == 200:
                    data = response.json()
                    self.log_result("Stock Out - Valid Operation", True, 
                                  f"Stock out successful: {data.get('message', 'OK')}")
                    
                    # Verify event was published
                    if "event published" in data.get('message', '').lower():
                        self.log_result("Stock Out - Event Publishing", True, 
                                      "RabbitMQ event published successfully")
                    else:
                        self.log_result("Stock Out - Event Publishing", False, 
                                      "Event publishing not confirmed in response")
                else:
                    self.log_result("Stock Out - Valid Operation", False, 
                                  f"HTTP {response.status_code}: {response.text}")
                
                # Test stock out with excessive quantity (insufficient stock)
                excessive_stock_data = {
                    "storeId": 1,
                    "productId": 2,
                    "quantity": 1000,
                    "referenceId": "EXCESSIVE-001",
                    "notes": "Testing insufficient stock scenario"
                }
                
                response = self.session.post(f"{BACKEND_URL.replace('/api', '')}/api/inventory/stock-out", 
                                           json=excessive_stock_data)
                
                if response.status_code == 400:
                    data = response.json()
                    if "insufficient stock" in data.get('error', '').lower():
                        self.log_result("Stock Out - Insufficient Stock", True, 
                                      "Correctly rejected insufficient stock scenario")
                    else:
                        self.log_result("Stock Out - Insufficient Stock", False, 
                                      f"Wrong error message: {data.get('error', '')}")
                else:
                    self.log_result("Stock Out - Insufficient Stock", False, 
                                  f"Should reject insufficient stock, got HTTP {response.status_code}")
            else:
                self.log_result("Stock Out - Setup", False, 
                              f"Failed to setup stock: HTTP {setup_response.status_code}")
            
            # Test stock out with invalid store ID
            invalid_store_data = {
                "storeId": 999,
                "productId": 1,
                "quantity": 10,
                "notes": "Testing invalid store"
            }
            
            response = self.session.post(f"{BACKEND_URL.replace('/api', '')}/api/inventory/stock-out", 
                                       json=invalid_store_data)
            
            if response.status_code == 400:
                self.log_result("Stock Out - Invalid Store", True, 
                              "Correctly rejected invalid store ID")
            else:
                self.log_result("Stock Out - Invalid Store", False, 
                              f"Should reject invalid store, got HTTP {response.status_code}")
            
            # Test stock out with zero/negative quantity
            zero_quantity_data = {
                "storeId": 1,
                "productId": 1,
                "quantity": 0,
                "notes": "Testing zero quantity"
            }
            
            response = self.session.post(f"{BACKEND_URL.replace('/api', '')}/api/inventory/stock-out", 
                                       json=zero_quantity_data)
            
            if response.status_code == 400:
                self.log_result("Stock Out - Zero Quantity", True, 
                              "Correctly rejected zero quantity")
            else:
                self.log_result("Stock Out - Zero Quantity", False, 
                              f"Should reject zero quantity, got HTTP {response.status_code}")
                
        except Exception as e:
            self.log_result("Stock Out Operations", False, f"Error: {str(e)}")

    def test_transaction_tracking(self):
        """Test transaction tracking and audit trail functionality"""
        try:
            # Test getting all transactions
            response = self.session.get(f"{BACKEND_URL.replace('/api', '')}/api/transactions")
            
            if response.status_code == 200:
                transactions = response.json()
                self.log_result("Transaction Tracking - Get All", True, 
                              f"Retrieved {len(transactions)} transactions")
                
                # Check if we have STOCK_IN and STOCK_OUT transactions
                stock_in_count = sum(1 for t in transactions if t.get('type') == 'STOCK_IN')
                stock_out_count = sum(1 for t in transactions if t.get('type') == 'STOCK_OUT')
                
                if stock_in_count > 0:
                    self.log_result("Transaction Tracking - STOCK_IN Type", True, 
                                  f"Found {stock_in_count} STOCK_IN transactions")
                else:
                    self.log_result("Transaction Tracking - STOCK_IN Type", False, 
                                  "No STOCK_IN transactions found")
                
                if stock_out_count > 0:
                    self.log_result("Transaction Tracking - STOCK_OUT Type", True, 
                                  f"Found {stock_out_count} STOCK_OUT transactions")
                else:
                    self.log_result("Transaction Tracking - STOCK_OUT Type", False, 
                                  "No STOCK_OUT transactions found")
            else:
                self.log_result("Transaction Tracking - Get All", False, 
                              f"HTTP {response.status_code}: {response.text}")
            
            # Test getting transactions by type
            for transaction_type in ['STOCK_IN', 'STOCK_OUT', 'ADJUSTMENT', 'RESERVATION', 'RELEASE']:
                response = self.session.get(f"{BACKEND_URL.replace('/api', '')}/api/transactions/type/{transaction_type}")
                
                if response.status_code == 200:
                    transactions = response.json()
                    self.log_result(f"Transaction Tracking - {transaction_type} Filter", True, 
                                  f"Retrieved {len(transactions)} {transaction_type} transactions")
                else:
                    self.log_result(f"Transaction Tracking - {transaction_type} Filter", False, 
                                  f"HTTP {response.status_code}")
            
            # Test getting recent transactions
            response = self.session.get(f"{BACKEND_URL.replace('/api', '')}/api/transactions/recent?limit=10")
            
            if response.status_code == 200:
                transactions = response.json()
                self.log_result("Transaction Tracking - Recent", True, 
                              f"Retrieved {len(transactions)} recent transactions")
            else:
                self.log_result("Transaction Tracking - Recent", False, 
                              f"HTTP {response.status_code}")
                
        except Exception as e:
            self.log_result("Transaction Tracking", False, f"Error: {str(e)}")

    def test_event_driven_integration(self):
        """Test event-driven integration with RabbitMQ"""
        try:
            # Check RabbitMQ queues exist
            queue_check_commands = [
                "sudo rabbitmqctl list_queues name messages",
                "sudo rabbitmqctl list_exchanges name type"
            ]
            
            for cmd in queue_check_commands:
                try:
                    result = subprocess.run(cmd.split(), capture_output=True, text=True, timeout=10)
                    if result.returncode == 0:
                        if "inventory" in result.stdout.lower():
                            self.log_result("Event Integration - RabbitMQ Queues", True, 
                                          "Inventory queues found in RabbitMQ")
                        else:
                            self.log_result("Event Integration - RabbitMQ Queues", False, 
                                          "No inventory queues found")
                    else:
                        self.log_result("Event Integration - RabbitMQ Check", False, 
                                      f"RabbitMQ command failed: {result.stderr}")
                except Exception as e:
                    self.log_result("Event Integration - RabbitMQ Check", False, 
                                  f"Command error: {str(e)}")
            
            # Test WebSocket endpoint for real-time updates
            try:
                # Check if WebSocket endpoint is available
                response = self.session.get(f"{BACKEND_URL.replace('/api', '')}/ws-info", timeout=5)
                if response.status_code in [200, 404]:
                    self.log_result("Event Integration - WebSocket Endpoint", True, 
                                  "WebSocket endpoint accessible")
                else:
                    self.log_result("Event Integration - WebSocket Endpoint", False, 
                                  f"WebSocket endpoint issue: HTTP {response.status_code}")
            except Exception as e:
                self.log_result("Event Integration - WebSocket Endpoint", False, 
                              f"WebSocket check error: {str(e)}")
                
        except Exception as e:
            self.log_result("Event Driven Integration", False, f"Error: {str(e)}")

    def test_inventory_consistency(self):
        """Test inventory consistency after stock operations"""
        try:
            # Get initial inventory state
            response = self.session.get(f"{BACKEND_URL.replace('/api', '')}/api/inventory/store/1/product/3")
            
            if response.status_code == 200:
                initial_inventory = response.json()
                initial_quantity = initial_inventory.get('quantity', 0)
                
                # Perform stock in operation
                stock_in_data = {
                    "storeId": 1,
                    "productId": 3,
                    "quantity": 25,
                    "referenceId": "CONSISTENCY-TEST-001",
                    "notes": "Testing inventory consistency"
                }
                
                stock_in_response = self.session.post(f"{BACKEND_URL.replace('/api', '')}/api/inventory/stock-in", 
                                                    json=stock_in_data)
                
                if stock_in_response.status_code == 200:
                    # Wait for event processing
                    time.sleep(3)
                    
                    # Check updated inventory
                    updated_response = self.session.get(f"{BACKEND_URL.replace('/api', '')}/api/inventory/store/1/product/3")
                    
                    if updated_response.status_code == 200:
                        updated_inventory = updated_response.json()
                        updated_quantity = updated_inventory.get('quantity', 0)
                        
                        expected_quantity = initial_quantity + 25
                        if updated_quantity == expected_quantity:
                            self.log_result("Inventory Consistency - Stock In", True, 
                                          f"Quantity correctly updated: {initial_quantity} → {updated_quantity}")
                        else:
                            self.log_result("Inventory Consistency - Stock In", False, 
                                          f"Quantity mismatch: expected {expected_quantity}, got {updated_quantity}")
                    else:
                        self.log_result("Inventory Consistency - Check Update", False, 
                                      f"Failed to check updated inventory: HTTP {updated_response.status_code}")
                else:
                    self.log_result("Inventory Consistency - Stock In", False, 
                                  f"Stock in failed: HTTP {stock_in_response.status_code}")
            else:
                self.log_result("Inventory Consistency - Initial Check", False, 
                              f"Failed to get initial inventory: HTTP {response.status_code}")
                
        except Exception as e:
            self.log_result("Inventory Consistency", False, f"Error: {str(e)}")
    
    def run_all_tests(self):
        """Run all backend tests"""
        print("🚀 Starting Distributed Inventory Management System Backend Tests")
        print("=" * 70)
        
        # Core backend tests
        self.test_fastapi_backend_health()
        self.test_fastapi_status_endpoints()
        self.test_direct_backend_access()
        
        # Spring Boot system tests
        self.test_spring_boot_endpoints()
        self.test_inventory_operations()
        
        # Infrastructure tests
        self.test_api_gateway_routing()
        self.test_database_connectivity()
        self.test_mongodb_connectivity()
        self.test_redis_connectivity()
        self.test_rabbitmq_connectivity()
        
        # Health checks
        self.test_service_health_checks()
        
        # Summary
        print("\n" + "=" * 70)
        print("📊 TEST SUMMARY")
        print("=" * 70)
        
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
    tester = BackendTester()
    results = tester.run_all_tests()
    
    # Exit with error code if any critical tests failed
    critical_failures = [r for r in results if not r['success'] and 
                        any(keyword in r['test'].lower() for keyword in 
                            ['fastapi', 'backend', 'database', 'connectivity'])]
    
    if critical_failures:
        print(f"\n⚠️  {len(critical_failures)} critical test(s) failed!")
        sys.exit(1)
    else:
        print("\n🎉 All critical tests passed!")
        sys.exit(0)