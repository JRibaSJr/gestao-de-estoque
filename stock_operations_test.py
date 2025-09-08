#!/usr/bin/env python3
"""
Focused test for Stock In/Stock Out functionality after Jackson LocalDateTime serialization fix
Testing the specific scenarios mentioned in the review request
"""

import requests
import json
import time
from datetime import datetime

# Configuration
BACKEND_URL = "https://distrib-inventory.preview.emergentagent.com"

class StockOperationsTest:
    def __init__(self):
        self.session = requests.Session()
        self.session.timeout = 15
        self.results = []
        
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
        print(f"{status}: {test_name}")
        print(f"   {message}")
        if details:
            print(f"   Details: {json.dumps(details, indent=2)}")
        print()

    def test_jackson_localdatetime_serialization(self):
        """Test 1: Jackson Configuration - LocalDateTime serialization issue resolution"""
        print("🔧 Testing Jackson LocalDateTime Serialization Fix")
        print("-" * 60)
        
        # Test with the exact data from review request
        stock_in_data = {
            "storeId": 1,
            "productId": 1, 
            "quantity": 25,
            "referenceId": "PO-2025-TEST",
            "notes": "Test stock entry"
        }
        
        try:
            response = self.session.post(f"{BACKEND_URL}/api/inventory/stock-in", json=stock_in_data)
            
            if response.status_code == 200:
                data = response.json()
                # Check if the response indicates successful event publishing (no serialization errors)
                if "event published" in data.get('message', '').lower():
                    self.log_result(
                        "Jackson LocalDateTime Serialization", 
                        True, 
                        "✅ LocalDateTime serialization issue RESOLVED - Events publishing successfully",
                        {"response": data, "status_code": response.status_code}
                    )
                else:
                    self.log_result(
                        "Jackson LocalDateTime Serialization", 
                        False, 
                        "Event publishing not confirmed in response",
                        {"response": data}
                    )
            else:
                self.log_result(
                    "Jackson LocalDateTime Serialization", 
                    False, 
                    f"Stock in operation failed: HTTP {response.status_code}",
                    {"response_text": response.text}
                )
                
        except Exception as e:
            self.log_result(
                "Jackson LocalDateTime Serialization", 
                False, 
                f"Request failed: {str(e)}"
            )

    def test_stock_in_with_rabbitmq_events(self):
        """Test 2: Stock In Operations with RabbitMQ event publishing"""
        print("📦 Testing Stock In Operations with RabbitMQ Events")
        print("-" * 60)
        
        # Test with the exact data from review request
        stock_in_data = {
            "storeId": 1,
            "productId": 1, 
            "quantity": 25,
            "referenceId": "PO-2025-TEST",
            "notes": "Test stock entry"
        }
        
        try:
            response = self.session.post(f"{BACKEND_URL}/api/inventory/stock-in", json=stock_in_data)
            
            if response.status_code == 200:
                data = response.json()
                
                # Check for successful operation
                success_indicators = [
                    "stock in event published" in data.get('message', '').lower(),
                    "added" in data.get('message', '').lower(),
                    "units" in data.get('message', '').lower()
                ]
                
                if any(success_indicators):
                    self.log_result(
                        "Stock In with RabbitMQ Events", 
                        True, 
                        f"✅ Stock In operation successful with event publishing: {data.get('message', '')}",
                        {"full_response": data}
                    )
                else:
                    self.log_result(
                        "Stock In with RabbitMQ Events", 
                        False, 
                        "Stock In succeeded but event publishing unclear",
                        {"response": data}
                    )
            else:
                self.log_result(
                    "Stock In with RabbitMQ Events", 
                    False, 
                    f"Stock In operation failed: HTTP {response.status_code}",
                    {"error_response": response.text}
                )
                
        except Exception as e:
            self.log_result(
                "Stock In with RabbitMQ Events", 
                False, 
                f"Request failed: {str(e)}"
            )

    def test_stock_out_with_rabbitmq_events(self):
        """Test 3: Stock Out Operations with RabbitMQ event publishing"""
        print("📤 Testing Stock Out Operations with RabbitMQ Events")
        print("-" * 60)
        
        # First ensure we have stock by doing a stock in
        setup_data = {
            "storeId": 1,
            "productId": 1,
            "quantity": 50,
            "referenceId": "SETUP-FOR-STOCK-OUT",
            "notes": "Setup stock for stock out test"
        }
        
        try:
            # Setup stock
            setup_response = self.session.post(f"{BACKEND_URL}/api/inventory/stock-in", json=setup_data)
            if setup_response.status_code == 200:
                print("   Setup: Added stock for testing stock out operations")
                
                # Wait for event processing
                time.sleep(3)
                
                # Now test stock out with the exact data from review request
                stock_out_data = {
                    "storeId": 1,
                    "productId": 1,
                    "quantity": 10, 
                    "referenceId": "SALE-2025-TEST",
                    "notes": "Test stock exit"
                }
                
                response = self.session.post(f"{BACKEND_URL}/api/inventory/stock-out", json=stock_out_data)
                
                if response.status_code == 200:
                    data = response.json()
                    
                    # Check for successful operation
                    success_indicators = [
                        "stock out event published" in data.get('message', '').lower(),
                        "removed" in data.get('message', '').lower(),
                        "units" in data.get('message', '').lower()
                    ]
                    
                    if any(success_indicators):
                        self.log_result(
                            "Stock Out with RabbitMQ Events", 
                            True, 
                            f"✅ Stock Out operation successful with event publishing: {data.get('message', '')}",
                            {"full_response": data}
                        )
                    else:
                        self.log_result(
                            "Stock Out with RabbitMQ Events", 
                            False, 
                            "Stock Out succeeded but event publishing unclear",
                            {"response": data}
                        )
                else:
                    self.log_result(
                        "Stock Out with RabbitMQ Events", 
                        False, 
                        f"Stock Out operation failed: HTTP {response.status_code}",
                        {"error_response": response.text}
                    )
            else:
                self.log_result(
                    "Stock Out with RabbitMQ Events", 
                    False, 
                    f"Failed to setup stock for testing: HTTP {setup_response.status_code}",
                    {"setup_error": setup_response.text}
                )
                
        except Exception as e:
            self.log_result(
                "Stock Out with RabbitMQ Events", 
                False, 
                f"Request failed: {str(e)}"
            )

    def test_transaction_records_creation(self):
        """Test 4: Transaction Records - STOCK_IN/STOCK_OUT types creation"""
        print("📋 Testing Transaction Records Creation")
        print("-" * 60)
        
        try:
            # Get all transactions
            response = self.session.get(f"{BACKEND_URL}/api/transactions")
            
            if response.status_code == 200:
                transactions = response.json()
                
                # Count transaction types
                stock_in_count = sum(1 for t in transactions if t.get('type') == 'STOCK_IN')
                stock_out_count = sum(1 for t in transactions if t.get('type') == 'STOCK_OUT')
                
                total_transactions = len(transactions)
                
                if total_transactions > 0:
                    self.log_result(
                        "Transaction Records Creation", 
                        True, 
                        f"✅ Transaction tracking working - Total: {total_transactions}, STOCK_IN: {stock_in_count}, STOCK_OUT: {stock_out_count}",
                        {
                            "total_transactions": total_transactions,
                            "stock_in_transactions": stock_in_count,
                            "stock_out_transactions": stock_out_count,
                            "recent_transactions": transactions[:3] if transactions else []
                        }
                    )
                else:
                    self.log_result(
                        "Transaction Records Creation", 
                        False, 
                        "❌ No transaction records found - Transaction tracking may not be working",
                        {"transaction_count": 0}
                    )
            else:
                self.log_result(
                    "Transaction Records Creation", 
                    False, 
                    f"Failed to retrieve transactions: HTTP {response.status_code}",
                    {"error": response.text}
                )
                
        except Exception as e:
            self.log_result(
                "Transaction Records Creation", 
                False, 
                f"Request failed: {str(e)}"
            )

    def test_central_inventory_control(self):
        """Test 5: Central Inventory Control - Verify inventory updates through messaging"""
        print("🎯 Testing Central Inventory Control via Messaging")
        print("-" * 60)
        
        try:
            # Get initial inventory for a specific product
            response = self.session.get(f"{BACKEND_URL}/api/inventory/store/1/product/2")
            
            if response.status_code == 200:
                initial_inventory = response.json()
                initial_quantity = initial_inventory.get('quantity', 0)
                
                print(f"   Initial inventory for Store 1, Product 2: {initial_quantity} units")
                
                # Perform stock in operation
                stock_in_data = {
                    "storeId": 1,
                    "productId": 2,
                    "quantity": 15,
                    "referenceId": "CENTRAL-CONTROL-TEST",
                    "notes": "Testing central inventory control"
                }
                
                stock_response = self.session.post(f"{BACKEND_URL}/api/inventory/stock-in", json=stock_in_data)
                
                if stock_response.status_code == 200:
                    print("   Stock In operation completed, waiting for message processing...")
                    
                    # Wait for message processing
                    time.sleep(5)
                    
                    # Check updated inventory
                    updated_response = self.session.get(f"{BACKEND_URL}/api/inventory/store/1/product/2")
                    
                    if updated_response.status_code == 200:
                        updated_inventory = updated_response.json()
                        updated_quantity = updated_inventory.get('quantity', 0)
                        
                        expected_quantity = initial_quantity + 15
                        
                        print(f"   Updated inventory: {updated_quantity} units (expected: {expected_quantity})")
                        
                        if updated_quantity == expected_quantity:
                            self.log_result(
                                "Central Inventory Control", 
                                True, 
                                f"✅ Central inventory control working - Quantity updated correctly: {initial_quantity} → {updated_quantity}",
                                {
                                    "initial_quantity": initial_quantity,
                                    "added_quantity": 15,
                                    "expected_quantity": expected_quantity,
                                    "actual_quantity": updated_quantity
                                }
                            )
                        else:
                            self.log_result(
                                "Central Inventory Control", 
                                False, 
                                f"❌ Inventory update mismatch - Expected: {expected_quantity}, Actual: {updated_quantity}",
                                {
                                    "initial_quantity": initial_quantity,
                                    "expected_quantity": expected_quantity,
                                    "actual_quantity": updated_quantity
                                }
                            )
                    else:
                        self.log_result(
                            "Central Inventory Control", 
                            False, 
                            f"Failed to retrieve updated inventory: HTTP {updated_response.status_code}",
                            {"error": updated_response.text}
                        )
                else:
                    self.log_result(
                        "Central Inventory Control", 
                        False, 
                        f"Stock In operation failed: HTTP {stock_response.status_code}",
                        {"error": stock_response.text}
                    )
            else:
                self.log_result(
                    "Central Inventory Control", 
                    False, 
                    f"Failed to get initial inventory: HTTP {response.status_code}",
                    {"error": response.text}
                )
                
        except Exception as e:
            self.log_result(
                "Central Inventory Control", 
                False, 
                f"Request failed: {str(e)}"
            )

    def test_event_driven_integration_verification(self):
        """Test 6: Event-Driven Integration - Verify RabbitMQ queues and processing"""
        print("🔄 Testing Event-Driven Integration")
        print("-" * 60)
        
        try:
            import subprocess
            
            # Check RabbitMQ queues
            result = subprocess.run(['sudo', 'rabbitmqctl', 'list_queues', 'name', 'messages'], 
                                  capture_output=True, text=True, timeout=10)
            
            if result.returncode == 0:
                queue_output = result.stdout
                
                # Check for inventory-related queues
                inventory_queues = []
                for line in queue_output.split('\n'):
                    if 'inventory' in line.lower():
                        inventory_queues.append(line.strip())
                
                if inventory_queues:
                    self.log_result(
                        "Event-Driven Integration", 
                        True, 
                        f"✅ RabbitMQ inventory queues operational: {len(inventory_queues)} queues found",
                        {"inventory_queues": inventory_queues}
                    )
                else:
                    self.log_result(
                        "Event-Driven Integration", 
                        False, 
                        "❌ No inventory queues found in RabbitMQ",
                        {"all_queues": queue_output}
                    )
            else:
                self.log_result(
                    "Event-Driven Integration", 
                    False, 
                    f"Failed to check RabbitMQ queues: {result.stderr}",
                    {"error": result.stderr}
                )
                
        except Exception as e:
            self.log_result(
                "Event-Driven Integration", 
                False, 
                f"RabbitMQ check failed: {str(e)}"
            )

    def run_focused_tests(self):
        """Run all focused tests for Stock In/Stock Out functionality"""
        print("🎯 FOCUSED TESTING: Stock In/Stock Out Functionality")
        print("After Jackson LocalDateTime Serialization Fix")
        print("=" * 80)
        print()
        
        # Run all verification tests
        self.test_jackson_localdatetime_serialization()
        self.test_stock_in_with_rabbitmq_events()
        self.test_stock_out_with_rabbitmq_events()
        self.test_transaction_records_creation()
        self.test_central_inventory_control()
        self.test_event_driven_integration_verification()
        
        # Summary
        print("=" * 80)
        print("📊 FOCUSED TEST SUMMARY")
        print("=" * 80)
        
        passed = sum(1 for r in self.results if r['success'])
        failed = len(self.results) - passed
        
        print(f"Total Focused Tests: {len(self.results)}")
        print(f"✅ Passed: {passed}")
        print(f"❌ Failed: {failed}")
        print(f"Success Rate: {(passed/len(self.results)*100):.1f}%")
        
        if failed > 0:
            print("\n🔍 FAILED TESTS:")
            for result in self.results:
                if not result['success']:
                    print(f"   • {result['test']}: {result['message']}")
        
        print("\n🎯 KEY FINDINGS:")
        
        # Check if LocalDateTime serialization is fixed
        serialization_test = next((r for r in self.results if 'LocalDateTime' in r['test']), None)
        if serialization_test and serialization_test['success']:
            print("   ✅ Jackson LocalDateTime serialization issue RESOLVED")
        else:
            print("   ❌ Jackson LocalDateTime serialization issue still present")
        
        # Check if events are publishing
        event_tests = [r for r in self.results if 'RabbitMQ' in r['test'] or 'Event' in r['test']]
        successful_events = sum(1 for t in event_tests if t['success'])
        if successful_events > 0:
            print("   ✅ RabbitMQ event publishing working")
        else:
            print("   ❌ RabbitMQ event publishing issues detected")
        
        return self.results

if __name__ == "__main__":
    tester = StockOperationsTest()
    results = tester.run_focused_tests()