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