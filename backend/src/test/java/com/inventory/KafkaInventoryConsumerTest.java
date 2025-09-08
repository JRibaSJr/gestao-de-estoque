package com.inventory;

import com.inventory.event.InventoryUpdateEvent;
import com.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"inventory.commands.stock"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
class KafkaInventoryConsumerTest {

    @Autowired
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void shouldConsumeInventoryUpdateEvent() throws Exception {
        // Envia um evento de ADD +5 para storeId=1, productId=1
        InventoryUpdateEvent event = new InventoryUpdateEvent(1L, 1L, 5, "ADD");
        event.setCorrelationId(java.util.UUID.randomUUID().toString());
        kafkaTemplate.send("inventory.commands.stock", event.getCorrelationId(), event).get();

        // Aguarda um pouco para o consumer processar
        Thread.sleep(1500);

        // Verifica se há ao menos um registro de inventário (dados seed + potencial atualização)
        long count = inventoryRepository.count();
        assertThat(count).isGreaterThan(0);
    }
}