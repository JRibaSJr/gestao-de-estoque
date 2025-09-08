# Diagrama ASCII – EDA (Kafka/Redpanda)

```
                           +-------------------+
                           |  Frontend (React) |
                           +---------+---------+
                                     |
                                     |  HTTP (via API Gateway /api/*)
                                     v
 +-------------------+       +-------+--------+       +-------------------------+
 |  Usuário/Browser  | ----> | Spring Cloud  | -----> |  Backend (Spring Boot)  |
 |                   |       | Gateway (8080)|        |  Porta interna 8001     |
 +-------------------+       +-------+--------+       +-----------+-------------+
                                                WebSocket        |
                                             /topic/*            |
                                                                 v
                                                    +------------+------------+
                                                    |      Kafka (Redpanda)   |
                                                    |      :9092              |
                                                    +------------+------------+
                                                                 |
         +----------------------------- Topics ------------------+-----------------------------+
         |                                                                                     |
         v                                                                                     v
 +--------------------------+                                                       +---------------------------+
 | inventory.commands.stock |  (3 partições)                                       | transfers.commands        |
 | (comandos de estoque)    | <--- Publish: InventoryEventPublisher (KafkaTemplate)| (3 partições)             |
 +------------+-------------+                                                       +------------+--------------+
              |                                                                              |
              | @KafkaListener                                                                | @KafkaListener
              v                                                                              v
 +------------------------------+                                       +----------------------------------+
 | InventoryUpdateConsumer      |                                       | InventoryTransferConsumer        |
 | - Aplica ADD/SUB/SET         |                                       | - Orquestra START/RESERVE/CONFIRM|
 | - STOCK_IN / STOCK_OUT       |                                       |   via InventoryTransferSaga      |
 +------------------------------+                                       +----------------------------------+
              |
              | publica auditoria
              v
 +------------------------------+              +----------------------------------+
 | notifications.events         | (1 partição) | inventory.events (3 partições)   |
 +------------------------------+              +----------------------------------+
              |                                            (sync/eventos gerais)
              | @KafkaListener
              v
 +------------------------------+
 | InventoryAuditConsumer       |
 | - Persiste SyncEvent (aud.)  |
 +------------------------------+

DLQ: inventory.dlq (1 partição) – uso futuro para redirecionamento de falhas graves.
```