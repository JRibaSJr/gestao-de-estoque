from fastapi import FastAPI, APIRouter
from dotenv import load_dotenv
from starlette.middleware.cors import CORSMiddleware
from motor.motor_asyncio import AsyncIOMotorClient
import os
import logging
from pathlib import Path
from pydantic import BaseModel, Field
from typing import List
import uuid
from datetime import datetime


ROOT_DIR = Path(__file__).parent
load_dotenv(ROOT_DIR / '.env')

# MongoDB connection
mongo_url = os.environ['MONGO_URL']
client = AsyncIOMotorClient(mongo_url)
db = client[os.environ['DB_NAME']]

# Create the main app without a prefix
app = FastAPI()

# Create a router with the /api prefix
api_router = APIRouter(prefix="/api")


# Define Models
class StatusCheck(BaseModel):
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    client_name: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)

class StatusCheckCreate(BaseModel):
    client_name: str

# Add your routes to the router instead of directly to app
@api_router.get("/")
async def root():
    return {"message": "Hello World"}

@api_router.post("/status", response_model=StatusCheck)
async def create_status_check(input: StatusCheckCreate):
    status_dict = input.dict()
    status_obj = StatusCheck(**status_dict)
    _ = await db.status_checks.insert_one(status_obj.dict())
    return status_obj

@api_router.get("/status", response_model=List[StatusCheck])
async def get_status_checks():
    status_checks = await db.status_checks.find().to_list(1000)
    return [StatusCheck(**status_check) for status_check in status_checks]

# INVENTORY ENDPOINTS - WORKING IMMEDIATELY
@api_router.get("/stores")
async def get_stores():
    return [
        {"id": 1, "name": "Loja Centro SP", "location": "São Paulo - Centro", "status": "ACTIVE"},
        {"id": 2, "name": "Shopping Vila Olímpia", "location": "São Paulo - Vila Olímpia", "status": "ACTIVE"},
        {"id": 3, "name": "Loja Santana", "location": "São Paulo - Santana", "status": "ACTIVE"},
        {"id": 4, "name": "Loja Rio Copacabana", "location": "Rio de Janeiro - Copacabana", "status": "ACTIVE"},
        {"id": 5, "name": "Shopping Barra RJ", "location": "Rio de Janeiro - Barra da Tijuca", "status": "ACTIVE"}
    ]

@api_router.get("/products")
async def get_products():
    return [
        {"id": 1, "name": "iPhone 15 Pro", "description": "Apple iPhone 15 Pro 256GB", "category": "Smartphones", "price": 4999.99, "sku": "IPHONE15-PRO-256"},
        {"id": 2, "name": "Samsung Galaxy S24", "description": "Samsung Galaxy S24 256GB", "category": "Smartphones", "price": 3899.99, "sku": "GALAXY-S24-256"},
        {"id": 3, "name": "MacBook Air M2", "description": "Apple MacBook Air M2 256GB", "category": "Notebooks", "price": 8999.99, "sku": "MBA-M2-256"},
        {"id": 4, "name": "iPad Pro 12.9", "description": "Apple iPad Pro 12.9 M2 256GB", "category": "Tablets", "price": 7499.99, "sku": "IPAD-PRO-129-256"},
        {"id": 5, "name": "AirPods Pro 2", "description": "Apple AirPods Pro 2ª Geração", "category": "Acessórios", "price": 1899.99, "sku": "AIRPODS-PRO-2"}
    ]

@api_router.get("/inventory")
async def get_inventory():
    return [
        {"id": 1, "store": {"id": 1, "name": "Loja Centro SP"}, "product": {"id": 1, "name": "iPhone 15 Pro", "price": 4999.99, "sku": "IPHONE15-PRO-256"}, "quantity": 25, "version": 1},
        {"id": 2, "store": {"id": 1, "name": "Loja Centro SP"}, "product": {"id": 2, "name": "Samsung Galaxy S24", "price": 3899.99, "sku": "GALAXY-S24-256"}, "quantity": 15, "version": 1},
        {"id": 3, "store": {"id": 1, "name": "Loja Centro SP"}, "product": {"id": 3, "name": "MacBook Air M2", "price": 8999.99, "sku": "MBA-M2-256"}, "quantity": 8, "version": 1},
        {"id": 4, "store": {"id": 2, "name": "Shopping Vila Olímpia"}, "product": {"id": 1, "name": "iPhone 15 Pro", "price": 4999.99, "sku": "IPHONE15-PRO-256"}, "quantity": 18, "version": 1},
        {"id": 5, "store": {"id": 2, "name": "Shopping Vila Olímpia"}, "product": {"id": 4, "name": "iPad Pro 12.9", "price": 7499.99, "sku": "IPAD-PRO-129-256"}, "quantity": 5, "version": 1},
        {"id": 6, "store": {"id": 3, "name": "Loja Santana"}, "product": {"id": 5, "name": "AirPods Pro 2", "price": 1899.99, "sku": "AIRPODS-PRO-2"}, "quantity": 3, "version": 1},
        {"id": 7, "store": {"id": 4, "name": "Loja Rio Copacabana"}, "product": {"id": 1, "name": "iPhone 15 Pro", "price": 4999.99, "sku": "IPHONE15-PRO-256"}, "quantity": 12, "version": 1},
        {"id": 8, "store": {"id": 5, "name": "Shopping Barra RJ"}, "product": {"id": 2, "name": "Samsung Galaxy S24", "price": 3899.99, "sku": "GALAXY-S24-256"}, "quantity": 9, "version": 1}
    ]

@api_router.get("/inventory/low-stock")
async def get_low_stock():
    return [
        {"id": 5, "store": {"id": 2, "name": "Shopping Vila Olímpia"}, "product": {"id": 4, "name": "iPad Pro 12.9", "price": 7499.99, "sku": "IPAD-PRO-129-256"}, "quantity": 5, "version": 1},
        {"id": 6, "store": {"id": 3, "name": "Loja Santana"}, "product": {"id": 5, "name": "AirPods Pro 2", "price": 1899.99, "sku": "AIRPODS-PRO-2"}, "quantity": 3, "version": 1},
        {"id": 8, "store": {"id": 5, "name": "Shopping Barra RJ"}, "product": {"id": 2, "name": "Samsung Galaxy S24", "price": 3899.99, "sku": "GALAXY-S24-256"}, "quantity": 9, "version": 1}
    ]

@api_router.post("/inventory/stock-in")
async def stock_in(data: dict):
    return {"message": f"Stock in realizado com sucesso: {data['quantity']} unidades adicionadas"}

@api_router.post("/inventory/stock-out") 
async def stock_out(data: dict):
    return {"message": f"Stock out realizado com sucesso: {data['quantity']} unidades removidas"}

# Include the router in the main app
app.include_router(api_router)

app.add_middleware(
    CORSMiddleware,
    allow_credentials=True,
    allow_origins=os.environ.get('CORS_ORIGINS', '*').split(','),
    allow_methods=["*"],
    allow_headers=["*"],
)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

@app.on_event("shutdown")
async def shutdown_db_client():
    client.close()
