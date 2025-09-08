package com.inventory.consumer;

import com.inventory.config.RabbitMQConfig;
import com.inventory.event.InventoryTransferEvent;
import com.inventory.saga.InventoryTransferSaga;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import com.rabbitmq.client.Channel;

@Service
public class InventoryTransferConsumer {

    @Autowired
    private InventoryTransferSaga transferSaga;

    @RabbitListener(queues = RabbitMQConfig.INVENTORY_TRANSFER_QUEUE)
    public void handleInventoryTransfer(InventoryTransferEvent event, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            System.out.println("🔄 Processing inventory transfer event: " + event.getEventId() + " - Type: " + event.getTransferType());
            
            // Route to appropriate saga step based on transfer type
            switch (event.getTransferType().toUpperCase()) {
                case "START":
                    transferSaga.startTransfer(event);
                    break;
                case "RESERVE":
                    transferSaga.reserveInventory(event);
                    break;
                case "CONFIRM":
                    transferSaga.confirmTransfer(event);
                    break;
                case "ROLLBACK":
                    transferSaga.rollbackTransfer(event);
                    break;
                default:
                    throw new RuntimeException("Unknown transfer type: " + event.getTransferType());
            }

            // ACK the message
            channel.basicAck(deliveryTag, false);
            
            System.out.println("✅ Successfully processed transfer event: " + event.getEventId());

        } catch (Exception e) {
            System.err.println("❌ Failed to process transfer event: " + event.getEventId() + " - " + e.getMessage());
            try {
                // NACK and don't requeue for critical failures
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackError) {
                System.err.println("❌ Failed to NACK message: " + nackError.getMessage());
            }
        }
    }
}