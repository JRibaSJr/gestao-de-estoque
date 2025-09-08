#!/usr/bin/env python3
"""
Simple Stock In/Stock Out Test - Direct API Testing
"""

import requests
import json
import time

# Configuration
BACKEND_URL = "https://distrib-inventory.preview.emergentagent.com"

def test_stock_operations():
    """Test stock in and stock out operations directly"""
    session = requests.Session()
    session.timeout = 10
    
    print("🧪 Testing Stock In/Stock Out Operations")
    print("=" * 50)
    
    # Test 1: Get current inventory state
    print("\n1. Getting current inventory state...")
    try:
        response = session.get(f"{BACKEND_URL}/api/inventory")
        if response.status_code == 200:
            inventory = response.json()
            print(f"✅ Current inventory has {len(inventory)} records")
            
            # Find a product with existing inventory
            test_store_id = 1
            test_product_id = 1
            current_quantity = 0
            
            for item in inventory:
                if item.get('storeId') == test_store_id and item.get('productId') == test_product_id:
                    current_quantity = item.get('quantity', 0)
                    break
            
            print(f"📦 Store {test_store_id}, Product {test_product_id}: Current quantity = {current_quantity}")
        else:
            print(f"❌ Failed to get inventory: HTTP {response.status_code}")
            return
    except Exception as e:
        print(f"❌ Error getting inventory: {e}")
        return
    
    # Test 2: Stock In Operation
    print("\n2. Testing Stock In Operation...")
    stock_in_data = {
        "storeId": test_store_id,
        "productId": test_product_id,
        "quantity": 25,
        "referenceId": "TEST-STOCK-IN-001",
        "notes": "Test stock in operation"
    }
    
    try:
        response = session.post(f"{BACKEND_URL}/api/inventory/stock-in", json=stock_in_data)
        print(f"Stock In Response: HTTP {response.status_code}")
        print(f"Response Body: {response.text}")
        
        if response.status_code == 200:
            print("✅ Stock In operation initiated successfully")
        else:
            print(f"❌ Stock In failed: HTTP {response.status_code}")
            
    except Exception as e:
        print(f"❌ Stock In error: {e}")
    
    # Test 3: Check transactions
    print("\n3. Checking transaction records...")
    try:
        response = session.get(f"{BACKEND_URL}/api/transactions")
        if response.status_code == 200:
            transactions = response.json()
            print(f"✅ Found {len(transactions)} total transactions")
            
            # Look for our test transaction
            test_transactions = [t for t in transactions if t.get('referenceId') == 'TEST-STOCK-IN-001']
            if test_transactions:
                print(f"✅ Found {len(test_transactions)} transactions with our reference ID")
                for t in test_transactions:
                    print(f"   - Type: {t.get('type')}, Quantity: {t.get('quantity')}, Timestamp: {t.get('timestamp')}")
            else:
                print("⚠️  No transactions found with our reference ID")
        else:
            print(f"❌ Failed to get transactions: HTTP {response.status_code}")
    except Exception as e:
        print(f"❌ Transaction check error: {e}")
    
    # Test 4: Stock Out Operation
    print("\n4. Testing Stock Out Operation...")
    stock_out_data = {
        "storeId": test_store_id,
        "productId": test_product_id,
        "quantity": 10,
        "referenceId": "TEST-STOCK-OUT-001",
        "notes": "Test stock out operation"
    }
    
    try:
        response = session.post(f"{BACKEND_URL}/api/inventory/stock-out", json=stock_out_data)
        print(f"Stock Out Response: HTTP {response.status_code}")
        print(f"Response Body: {response.text}")
        
        if response.status_code == 200:
            print("✅ Stock Out operation initiated successfully")
        elif response.status_code == 400:
            error_data = response.json()
            if "insufficient stock" in error_data.get('error', '').lower():
                print("✅ Stock Out correctly rejected due to insufficient stock")
            else:
                print(f"❌ Stock Out failed with unexpected error: {error_data.get('error')}")
        else:
            print(f"❌ Stock Out failed: HTTP {response.status_code}")
            
    except Exception as e:
        print(f"❌ Stock Out error: {e}")
    
    # Test 5: Check RabbitMQ queues
    print("\n5. Checking RabbitMQ integration...")
    try:
        import subprocess
        result = subprocess.run(['sudo', 'rabbitmqctl', 'list_queues', 'name', 'messages'], 
                              capture_output=True, text=True, timeout=10)
        if result.returncode == 0:
            print("✅ RabbitMQ queues status:")
            for line in result.stdout.split('\n'):
                if 'inventory' in line.lower():
                    print(f"   {line}")
        else:
            print(f"❌ RabbitMQ check failed: {result.stderr}")
    except Exception as e:
        print(f"❌ RabbitMQ check error: {e}")

if __name__ == "__main__":
    test_stock_operations()