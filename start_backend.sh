#!/bin/bash

echo "🚀 Starting Distributed Inventory System Backend..."

# Navigate to backend directory
cd /app/backend

# Compile the project
echo "📦 Compiling Spring Boot project..."
mvn clean package -DskipTests -q

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    
    # Start supervisor to manage backend service
    echo "🔄 Starting backend service with supervisor..."
    supervisorctl reread
    supervisorctl update
    supervisorctl start backend
    
    echo "🎯 Backend started successfully!"
    echo "📡 API will be available at: http://localhost:8001"
    echo "📚 Swagger UI: http://localhost:8001/api/swagger-ui.html"
    echo "🩺 Health Check: http://localhost:8001/api/actuator/health"
    
else
    echo "❌ Build failed! Check the logs above for errors."
    exit 1
fi