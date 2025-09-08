#!/usr/bin/env python3
"""
Static Code Analysis for Kafka Migration and H2 Database Configuration
Tests the Java/Spring Boot backend code for correct Kafka migration and H2 setup
"""

import os
import re
import xml.etree.ElementTree as ET
import yaml
from pathlib import Path

class KafkaMigrationTester:
    def __init__(self):
        self.results = []
        self.backend_path = Path("/app/backend")
        
    def log_result(self, test_name, success, message, details=None):
        """Log test result"""
        result = {
            'test': test_name,
            'success': success,
            'message': message,
            'details': details or {}
        }
        self.results.append(result)
        status = "✅ PASS" if success else "❌ FAIL"
        print(f"{status}: {test_name} - {message}")
        if details:
            print(f"   Details: {details}")
    
    def test_pom_xml_kafka_dependencies(self):
        """Test pom.xml uses spring-kafka not spring-amqp"""
        try:
            pom_path = self.backend_path / "pom.xml"
            if not pom_path.exists():
                self.log_result("POM.xml Kafka Dependencies", False, "pom.xml not found")
                return
            
            tree = ET.parse(pom_path)
            root = tree.getroot()
            
            # Find all dependencies
            dependencies = []
            for dep in root.findall(".//{http://maven.apache.org/POM/4.0.0}dependency"):
                group_id = dep.find("{http://maven.apache.org/POM/4.0.0}groupId")
                artifact_id = dep.find("{http://maven.apache.org/POM/4.0.0}artifactId")
                if group_id is not None and artifact_id is not None:
                    dependencies.append(f"{group_id.text}:{artifact_id.text}")
            
            # Check for spring-kafka
            kafka_deps = [dep for dep in dependencies if "spring-kafka" in dep]
            amqp_deps = [dep for dep in dependencies if "spring-amqp" in dep or "spring-rabbit" in dep]
            
            if kafka_deps:
                self.log_result("POM.xml Kafka Dependencies", True, 
                              f"Found spring-kafka dependency: {kafka_deps[0]}")
            else:
                self.log_result("POM.xml Kafka Dependencies", False, 
                              "spring-kafka dependency not found in pom.xml")
            
            # Check for remaining RabbitMQ dependencies (should only be in test scope)
            production_amqp_deps = []
            for dep in root.findall(".//{http://maven.apache.org/POM/4.0.0}dependency"):
                group_id = dep.find("{http://maven.apache.org/POM/4.0.0}groupId")
                artifact_id = dep.find("{http://maven.apache.org/POM/4.0.0}artifactId")
                scope = dep.find("{http://maven.apache.org/POM/4.0.0}scope")
                
                if (group_id is not None and artifact_id is not None and 
                    ("amqp" in group_id.text or "rabbit" in artifact_id.text)):
                    if scope is None or scope.text != "test":
                        production_amqp_deps.append(f"{group_id.text}:{artifact_id.text}")
            
            if not production_amqp_deps:
                self.log_result("POM.xml RabbitMQ Cleanup", True, 
                              "No production RabbitMQ dependencies found")
            else:
                self.log_result("POM.xml RabbitMQ Cleanup", False, 
                              f"Found production RabbitMQ dependencies: {production_amqp_deps}")
            
            # Check for H2 database dependency
            h2_deps = [dep for dep in dependencies if "h2database" in dep]
            if h2_deps:
                self.log_result("POM.xml H2 Database", True, 
                              f"Found H2 database dependency: {h2_deps[0]}")
            else:
                self.log_result("POM.xml H2 Database", False, 
                              "H2 database dependency not found")
                
        except Exception as e:
            self.log_result("POM.xml Analysis", False, f"Error parsing pom.xml: {str(e)}")
    
    def test_application_yml_h2_config(self):
        """Test application.yml uses H2 in-memory database with create-drop"""
        try:
            app_yml_path = self.backend_path / "src/main/resources/application.yml"
            if not app_yml_path.exists():
                self.log_result("Application.yml H2 Config", False, "application.yml not found")
                return
            
            with open(app_yml_path, 'r') as f:
                config = yaml.safe_load(f)
            
            # Check datasource configuration
            datasource = config.get('spring', {}).get('datasource', {})
            
            # Check H2 URL
            url = datasource.get('url', '')
            if 'jdbc:h2:mem:' in url:
                self.log_result("Application.yml H2 URL", True, 
                              f"H2 in-memory database configured: {url}")
            else:
                self.log_result("Application.yml H2 URL", False, 
                              f"H2 in-memory URL not found, got: {url}")
            
            # Check driver class
            driver = datasource.get('driver-class-name', '')
            if driver == 'org.h2.Driver':
                self.log_result("Application.yml H2 Driver", True, 
                              "H2 driver correctly configured")
            else:
                self.log_result("Application.yml H2 Driver", False, 
                              f"H2 driver not configured, got: {driver}")
            
            # Check JPA configuration
            jpa = config.get('spring', {}).get('jpa', {})
            hibernate = jpa.get('hibernate', {})
            ddl_auto = hibernate.get('ddl-auto', '')
            
            if ddl_auto == 'create-drop':
                self.log_result("Application.yml JPA DDL", True, 
                              "JPA ddl-auto set to create-drop")
            else:
                self.log_result("Application.yml JPA DDL", False, 
                              f"JPA ddl-auto not create-drop, got: {ddl_auto}")
            
            # Check Hibernate dialect
            properties = jpa.get('properties', {}).get('hibernate', {})
            dialect = properties.get('dialect', '')
            
            if 'H2Dialect' in dialect:
                self.log_result("Application.yml Hibernate Dialect", True, 
                              f"H2 dialect configured: {dialect}")
            else:
                self.log_result("Application.yml Hibernate Dialect", False, 
                              f"H2 dialect not configured, got: {dialect}")
            
            # Check for SQLite configurations (should be removed)
            sqlite_configs = []
            if 'sqlite' in str(config).lower():
                sqlite_configs.append("Found 'sqlite' references in config")
            
            if not sqlite_configs:
                self.log_result("Application.yml SQLite Cleanup", True, 
                              "No SQLite configurations found")
            else:
                self.log_result("Application.yml SQLite Cleanup", False, 
                              f"SQLite configurations still present: {sqlite_configs}")
            
            # Check Kafka configuration
            kafka = config.get('spring', {}).get('kafka', {})
            if kafka:
                bootstrap_servers = kafka.get('bootstrap-servers', '')
                if bootstrap_servers:
                    self.log_result("Application.yml Kafka Config", True, 
                                  f"Kafka bootstrap servers configured: {bootstrap_servers}")
                else:
                    self.log_result("Application.yml Kafka Config", False, 
                                  "Kafka bootstrap servers not configured")
            else:
                self.log_result("Application.yml Kafka Config", False, 
                              "Kafka configuration not found")
                
        except Exception as e:
            self.log_result("Application.yml Analysis", False, f"Error parsing application.yml: {str(e)}")
    
    def test_kafka_config_class(self):
        """Test KafkaConfig defines Producer/Consumer factories, topic builders, and listener factory"""
        try:
            kafka_config_path = self.backend_path / "src/main/java/com/inventory/config/KafkaConfig.java"
            if not kafka_config_path.exists():
                self.log_result("KafkaConfig Class", False, "KafkaConfig.java not found")
                return
            
            with open(kafka_config_path, 'r') as f:
                content = f.read()
            
            # Check for @Configuration annotation
            if '@Configuration' in content:
                self.log_result("KafkaConfig @Configuration", True, 
                              "KafkaConfig has @Configuration annotation")
            else:
                self.log_result("KafkaConfig @Configuration", False, 
                              "@Configuration annotation not found")
            
            # Check for ProducerFactory bean
            if 'ProducerFactory<String, Object>' in content and '@Bean' in content:
                self.log_result("KafkaConfig ProducerFactory", True, 
                              "ProducerFactory bean defined")
            else:
                self.log_result("KafkaConfig ProducerFactory", False, 
                              "ProducerFactory bean not found")
            
            # Check for ConsumerFactory bean
            if 'ConsumerFactory<String, Object>' in content and '@Bean' in content:
                self.log_result("KafkaConfig ConsumerFactory", True, 
                              "ConsumerFactory bean defined")
            else:
                self.log_result("KafkaConfig ConsumerFactory", False, 
                              "ConsumerFactory bean not found")
            
            # Check for KafkaTemplate bean
            if 'KafkaTemplate<String, Object>' in content and '@Bean' in content:
                self.log_result("KafkaConfig KafkaTemplate", True, 
                              "KafkaTemplate bean defined")
            else:
                self.log_result("KafkaConfig KafkaTemplate", False, 
                              "KafkaTemplate bean not found")
            
            # Check for ConcurrentKafkaListenerContainerFactory
            if 'ConcurrentKafkaListenerContainerFactory' in content:
                self.log_result("KafkaConfig ListenerFactory", True, 
                              "KafkaListenerContainerFactory defined")
            else:
                self.log_result("KafkaConfig ListenerFactory", False, 
                              "KafkaListenerContainerFactory not found")
            
            # Check for topic builders
            topic_patterns = ['TopicBuilder', 'NewTopic']
            topics_found = []
            for pattern in topic_patterns:
                if pattern in content:
                    topics_found.append(pattern)
            
            if topics_found:
                self.log_result("KafkaConfig Topic Builders", True, 
                              f"Topic builders found: {', '.join(topics_found)}")
            else:
                self.log_result("KafkaConfig Topic Builders", False, 
                              "Topic builders not found")
                
        except Exception as e:
            self.log_result("KafkaConfig Analysis", False, f"Error analyzing KafkaConfig: {str(e)}")
    
    def test_kafka_consumers(self):
        """Test consumers use @KafkaListener with proper topics and acknowledgment"""
        consumer_files = [
            "InventoryUpdateConsumer.java",
            "InventoryTransferConsumer.java", 
            "InventoryAuditConsumer.java"
        ]
        
        for consumer_file in consumer_files:
            try:
                consumer_path = self.backend_path / f"src/main/java/com/inventory/consumer/{consumer_file}"
                if not consumer_path.exists():
                    self.log_result(f"Consumer {consumer_file}", False, f"{consumer_file} not found")
                    continue
                
                with open(consumer_path, 'r') as f:
                    content = f.read()
                
                # Check for @KafkaListener annotation
                if '@KafkaListener' in content:
                    self.log_result(f"Consumer {consumer_file} @KafkaListener", True, 
                                  "@KafkaListener annotation found")
                else:
                    self.log_result(f"Consumer {consumer_file} @KafkaListener", False, 
                                  "@KafkaListener annotation not found")
                
                # Check for topics parameter
                if 'topics = {' in content or 'topics = "' in content:
                    self.log_result(f"Consumer {consumer_file} Topics", True, 
                                  "Topics parameter configured")
                else:
                    self.log_result(f"Consumer {consumer_file} Topics", False, 
                                  "Topics parameter not found")
                
                # Check for Acknowledgment parameter
                if 'Acknowledgment' in content:
                    self.log_result(f"Consumer {consumer_file} Acknowledgment", True, 
                                  "Acknowledgment parameter found")
                else:
                    self.log_result(f"Consumer {consumer_file} Acknowledgment", False, 
                                  "Acknowledgment parameter not found")
                
                # Check for groupId
                if 'groupId = "' in content:
                    self.log_result(f"Consumer {consumer_file} GroupId", True, 
                                  "GroupId configured")
                else:
                    self.log_result(f"Consumer {consumer_file} GroupId", False, 
                                  "GroupId not configured")
                
                # Check for RabbitMQ imports (should be removed)
                rabbitmq_imports = []
                lines = content.split('\n')
                for line in lines:
                    if 'import' in line and ('rabbit' in line.lower() or 'amqp' in line.lower()):
                        if not line.strip().startswith('//'):  # Not commented out
                            rabbitmq_imports.append(line.strip())
                
                if not rabbitmq_imports:
                    self.log_result(f"Consumer {consumer_file} RabbitMQ Cleanup", True, 
                                  "No RabbitMQ imports found")
                else:
                    self.log_result(f"Consumer {consumer_file} RabbitMQ Cleanup", False, 
                                  f"RabbitMQ imports still present: {rabbitmq_imports}")
                    
            except Exception as e:
                self.log_result(f"Consumer {consumer_file} Analysis", False, 
                              f"Error analyzing {consumer_file}: {str(e)}")
    
    def test_kafka_publisher(self):
        """Test publisher uses KafkaTemplate<String,Object>"""
        try:
            publisher_path = self.backend_path / "src/main/java/com/inventory/publisher/InventoryEventPublisher.java"
            if not publisher_path.exists():
                self.log_result("Publisher KafkaTemplate", False, "InventoryEventPublisher.java not found")
                return
            
            with open(publisher_path, 'r') as f:
                content = f.read()
            
            # Check for KafkaTemplate injection
            if 'KafkaTemplate<String, Object>' in content:
                self.log_result("Publisher KafkaTemplate Type", True, 
                              "KafkaTemplate<String, Object> found")
            else:
                self.log_result("Publisher KafkaTemplate Type", False, 
                              "KafkaTemplate<String, Object> not found")
            
            # Check for @Autowired KafkaTemplate
            if '@Autowired' in content and 'KafkaTemplate' in content:
                self.log_result("Publisher KafkaTemplate Injection", True, 
                              "KafkaTemplate properly injected")
            else:
                self.log_result("Publisher KafkaTemplate Injection", False, 
                              "KafkaTemplate injection not found")
            
            # Check for kafkaTemplate.send() calls
            if 'kafkaTemplate.send(' in content:
                self.log_result("Publisher Send Method", True, 
                              "kafkaTemplate.send() calls found")
            else:
                self.log_result("Publisher Send Method", False, 
                              "kafkaTemplate.send() calls not found")
            
            # Check for RabbitMQ references (should be removed)
            rabbitmq_refs = []
            if 'RabbitTemplate' in content:
                rabbitmq_refs.append("RabbitTemplate")
            if 'rabbitTemplate' in content:
                rabbitmq_refs.append("rabbitTemplate")
            
            if not rabbitmq_refs:
                self.log_result("Publisher RabbitMQ Cleanup", True, 
                              "No RabbitMQ references found")
            else:
                self.log_result("Publisher RabbitMQ Cleanup", False, 
                              f"RabbitMQ references still present: {rabbitmq_refs}")
                
        except Exception as e:
            self.log_result("Publisher Analysis", False, f"Error analyzing publisher: {str(e)}")
    
    def test_inventory_controller_api_routes(self):
        """Test API routes still prefixed with /api in InventoryController"""
        try:
            controller_path = self.backend_path / "src/main/java/com/inventory/controller/InventoryController.java"
            if not controller_path.exists():
                self.log_result("Controller API Routes", False, "InventoryController.java not found")
                return
            
            with open(controller_path, 'r') as f:
                content = f.read()
            
            # Check for @RequestMapping with /api prefix
            if '@RequestMapping("/api/inventory")' in content:
                self.log_result("Controller API Prefix", True, 
                              "@RequestMapping('/api/inventory') found")
            else:
                self.log_result("Controller API Prefix", False, 
                              "@RequestMapping('/api/inventory') not found")
            
            # Check for specific endpoints
            endpoints = [
                'stock-in',
                'stock-out', 
                'transfer',
                'update',
                'reserve',
                'low-stock'
            ]
            
            found_endpoints = []
            for endpoint in endpoints:
                if f'"{endpoint}"' in content or f"/{endpoint}" in content:
                    found_endpoints.append(endpoint)
            
            if len(found_endpoints) >= 4:  # Most endpoints found
                self.log_result("Controller Endpoints", True, 
                              f"Found endpoints: {', '.join(found_endpoints)}")
            else:
                self.log_result("Controller Endpoints", False, 
                              f"Only found {len(found_endpoints)} endpoints: {', '.join(found_endpoints)}")
                
        except Exception as e:
            self.log_result("Controller Analysis", False, f"Error analyzing controller: {str(e)}")
    
    def test_frontend_api_service(self):
        """Test frontend service api.js uses REACT_APP_BACKEND_URL + '/api'"""
        try:
            api_js_path = Path("/app/frontend/src/services/api.js")
            if not api_js_path.exists():
                self.log_result("Frontend API Service", False, "api.js not found")
                return
            
            with open(api_js_path, 'r') as f:
                content = f.read()
            
            # Check for REACT_APP_BACKEND_URL usage
            if 'REACT_APP_BACKEND_URL' in content:
                self.log_result("Frontend Backend URL", True, 
                              "REACT_APP_BACKEND_URL environment variable used")
            else:
                self.log_result("Frontend Backend URL", False, 
                              "REACT_APP_BACKEND_URL not found")
            
            # Check for /api suffix
            if '${BACKEND_URL}/api' in content or '`${' in content and '/api`' in content:
                self.log_result("Frontend API Suffix", True, 
                              "'/api' suffix properly appended to backend URL")
            else:
                self.log_result("Frontend API Suffix", False, 
                              "'/api' suffix not properly configured")
            
            # Check for inventory API endpoints
            inventory_endpoints = [
                'stock-in',
                'stock-out',
                'inventory',
                'stores',
                'products'
            ]
            
            found_api_endpoints = []
            for endpoint in inventory_endpoints:
                if endpoint in content:
                    found_api_endpoints.append(endpoint)
            
            if len(found_api_endpoints) >= 3:
                self.log_result("Frontend API Endpoints", True, 
                              f"Found API endpoints: {', '.join(found_api_endpoints)}")
            else:
                self.log_result("Frontend API Endpoints", False, 
                              f"Only found {len(found_api_endpoints)} API endpoints")
                
        except Exception as e:
            self.log_result("Frontend API Analysis", False, f"Error analyzing api.js: {str(e)}")
    
    def test_rabbitmq_config_disabled(self):
        """Test RabbitMQConfig is disabled (no @Configuration annotation)"""
        try:
            rabbitmq_config_path = self.backend_path / "src/main/java/com/inventory/config/RabbitMQConfig.java"
            if not rabbitmq_config_path.exists():
                self.log_result("RabbitMQConfig Disabled", True, 
                              "RabbitMQConfig.java not found (completely removed)")
                return
            
            with open(rabbitmq_config_path, 'r') as f:
                content = f.read()
            
            # Check if @Configuration is commented out
            lines = content.split('\n')
            config_commented = False
            for line in lines:
                if '@Configuration' in line and line.strip().startswith('//'):
                    config_commented = True
                    break
                elif '@Configuration' in line and not line.strip().startswith('//'):
                    self.log_result("RabbitMQConfig Disabled", False, 
                                  "@Configuration annotation still active")
                    return
            
            if config_commented:
                self.log_result("RabbitMQConfig Disabled", True, 
                              "@Configuration annotation commented out")
            else:
                # Check if class is marked as deprecated
                if 'Deprecated' in content or 'deprecated' in content:
                    self.log_result("RabbitMQConfig Disabled", True, 
                                  "RabbitMQConfig marked as deprecated")
                else:
                    self.log_result("RabbitMQConfig Disabled", False, 
                                  "RabbitMQConfig status unclear")
                    
        except Exception as e:
            self.log_result("RabbitMQConfig Analysis", False, f"Error analyzing RabbitMQConfig: {str(e)}")
    
    def test_missing_imports_compile_issues(self):
        """Check for missing imports or potential compile issues"""
        java_files = []
        
        # Find all Java files
        for root, dirs, files in os.walk(self.backend_path / "src/main/java"):
            for file in files:
                if file.endswith('.java'):
                    java_files.append(Path(root) / file)
        
        compile_issues = []
        
        for java_file in java_files:
            try:
                with open(java_file, 'r') as f:
                    content = f.read()
                
                # Check for common import issues
                lines = content.split('\n')
                imports = [line.strip() for line in lines if line.strip().startswith('import')]
                
                # Check for missing Kafka imports where @KafkaListener is used
                if '@KafkaListener' in content:
                    kafka_imports = [imp for imp in imports if 'kafka' in imp.lower()]
                    if not kafka_imports:
                        compile_issues.append(f"{java_file.name}: @KafkaListener used but no Kafka imports")
                
                # Check for RabbitMQ imports in active code
                for line in lines:
                    if ('import' in line and ('rabbit' in line.lower() or 'amqp' in line.lower()) 
                        and not line.strip().startswith('//')):
                        compile_issues.append(f"{java_file.name}: Active RabbitMQ import: {line.strip()}")
                
                # Check for undefined references
                if 'RabbitTemplate' in content and '@Autowired' in content:
                    rabbit_imports = [imp for imp in imports if 'rabbit' in imp.lower()]
                    if not rabbit_imports:
                        compile_issues.append(f"{java_file.name}: RabbitTemplate used but not imported")
                        
            except Exception as e:
                compile_issues.append(f"{java_file.name}: Error reading file - {str(e)}")
        
        if not compile_issues:
            self.log_result("Compile Issues Check", True, 
                          "No obvious compile issues found")
        else:
            self.log_result("Compile Issues Check", False, 
                          f"Found {len(compile_issues)} potential issues", 
                          {"issues": compile_issues})
    
    def run_all_tests(self):
        """Run all Kafka migration and H2 database tests"""
        print("🔍 Starting Kafka Migration and H2 Database Static Analysis")
        print("=" * 70)
        
        # 1. Kafka migration correctness
        print("\n📡 Testing Kafka Migration")
        print("-" * 40)
        self.test_pom_xml_kafka_dependencies()
        self.test_kafka_config_class()
        self.test_kafka_consumers()
        self.test_kafka_publisher()
        self.test_rabbitmq_config_disabled()
        
        # 2. H2 in-memory DB config
        print("\n🗄️ Testing H2 Database Configuration")
        print("-" * 40)
        self.test_application_yml_h2_config()
        
        # 3. API routes verification
        print("\n🛣️ Testing API Routes")
        print("-" * 40)
        self.test_inventory_controller_api_routes()
        self.test_frontend_api_service()
        
        # 4. Code quality checks
        print("\n🔧 Testing Code Quality")
        print("-" * 40)
        self.test_missing_imports_compile_issues()
        
        # Summary
        print("\n" + "=" * 70)
        print("📊 KAFKA MIGRATION TEST SUMMARY")
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
                    if result.get('details'):
                        for key, value in result['details'].items():
                            if isinstance(value, list):
                                for item in value[:3]:  # Show first 3 items
                                    print(f"     - {item}")
                                if len(value) > 3:
                                    print(f"     ... and {len(value) - 3} more")
        
        return self.results

if __name__ == "__main__":
    tester = KafkaMigrationTester()
    results = tester.run_all_tests()
    
    # Determine if migration was successful
    critical_tests = [
        "POM.xml Kafka Dependencies",
        "Application.yml H2 URL", 
        "KafkaConfig ProducerFactory",
        "KafkaConfig ConsumerFactory",
        "Controller API Prefix"
    ]
    
    critical_failures = [r for r in results if not r['success'] and r['test'] in critical_tests]
    
    if critical_failures:
        print(f"\n⚠️ {len(critical_failures)} critical migration test(s) failed!")
        exit(1)
    else:
        print("\n🎉 Kafka migration and H2 configuration verified successfully!")
        exit(0)