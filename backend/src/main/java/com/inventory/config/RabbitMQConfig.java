package com.inventory.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// @Configuration (removed after Kafka migration)
public class RabbitMQConfig { // Deprecated after Kafka migration

    // Exchange names
    public static final String INVENTORY_EXCHANGE = "inventory.exchange";
    public static final String DLX_EXCHANGE = "inventory.dlx.exchange";
    
    // Queue names
    public static final String INVENTORY_UPDATE_QUEUE = "inventory.update.queue";
    public static final String INVENTORY_TRANSFER_QUEUE = "inventory.transfer.queue";
    public static final String INVENTORY_SYNC_QUEUE = "inventory.sync.queue";
    public static final String INVENTORY_AUDIT_QUEUE = "inventory.audit.queue";
    
    // Dead Letter Queues
    public static final String INVENTORY_DLQ = "inventory.dlq";
    
    // Routing keys
    public static final String INVENTORY_UPDATE_KEY = "inventory.update";
    public static final String INVENTORY_TRANSFER_KEY = "inventory.transfer";
    public static final String INVENTORY_SYNC_KEY = "inventory.sync";
    public static final String INVENTORY_AUDIT_KEY = "inventory.audit";

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                System.err.println("Message not delivered to exchange: " + cause);
            }
        });
        template.setReturnsCallback(returned -> 
            System.err.println("Message returned: " + returned.getMessage())
        );
        return template;
    }

    // Main Exchange
    @Bean
    public TopicExchange inventoryExchange() {
        return new TopicExchange(INVENTORY_EXCHANGE, true, false);
    }

    // Dead Letter Exchange
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    // Inventory Update Queue (High Priority)
    @Bean
    public Queue inventoryUpdateQueue() {
        return QueueBuilder.durable(INVENTORY_UPDATE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .withArgument("x-max-priority", 10)
                .build();
    }

    @Bean
    public Binding inventoryUpdateBinding() {
        return BindingBuilder
                .bind(inventoryUpdateQueue())
                .to(inventoryExchange())
                .with(INVENTORY_UPDATE_KEY);
    }

    // Inventory Transfer Queue (Critical Priority)
    @Bean
    public Queue inventoryTransferQueue() {
        return QueueBuilder.durable(INVENTORY_TRANSFER_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .withArgument("x-max-priority", 15)
                .build();
    }

    @Bean
    public Binding inventoryTransferBinding() {
        return BindingBuilder
                .bind(inventoryTransferQueue())
                .to(inventoryExchange())
                .with(INVENTORY_TRANSFER_KEY);
    }

    // Inventory Sync Queue (Medium Priority)
    @Bean
    public Queue inventorySyncQueue() {
        return QueueBuilder.durable(INVENTORY_SYNC_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "dlq")
                .withArgument("x-max-priority", 5)
                .build();
    }

    @Bean
    public Binding inventorySyncBinding() {
        return BindingBuilder
                .bind(inventorySyncQueue())
                .to(inventoryExchange())
                .with(INVENTORY_SYNC_KEY);
    }

    // Inventory Audit Queue (Low Priority, but Persistent)
    @Bean
    public Queue inventoryAuditQueue() {
        return QueueBuilder.durable(INVENTORY_AUDIT_QUEUE)
                .withArgument("x-message-ttl", 86400000) // 24 hours TTL
                .build();
    }

    @Bean
    public Binding inventoryAuditBinding() {
        return BindingBuilder
                .bind(inventoryAuditQueue())
                .to(inventoryExchange())
                .with(INVENTORY_AUDIT_KEY);
    }

    // Dead Letter Queue
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(INVENTORY_DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dlq");
    }
}