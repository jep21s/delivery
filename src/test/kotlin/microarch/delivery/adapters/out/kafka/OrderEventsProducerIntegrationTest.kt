package microarch.delivery.adapters.out.kafka

import java.time.Duration
import java.util.Collections
import java.util.Properties
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS
import microarch.delivery.BaseIntegrationTest
import microarch.delivery.application.commands.AssignOrderCommand
import microarch.delivery.application.commands.AssignOrderCommandHandler
import microarch.delivery.application.commands.CompleteOrderCommand
import microarch.delivery.application.commands.CompleteOrderCommandHandler
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.ports.courier.CourierRepository
import microarch.delivery.core.ports.order.OrderRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import queues.order.events.OrderEventsProto.OrderAssignedIntegrationEvent
import queues.order.events.OrderEventsProto.OrderCompletedIntegrationEvent

class OrderEventsProducerIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var courierRepository: CourierRepository

    @Autowired
    private lateinit var assignOrderCommandHandler: AssignOrderCommandHandler

    @Autowired
    private lateinit var completeOrderCommandHandler: CompleteOrderCommandHandler

    @Value("\${app.kafka.order-events-topic}")
    private lateinit var topic: String

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Test
    fun `when order is assigned then OrderAssignedIntegrationEvent is published`() {
        // Given — заказ и курьер в одной локации, плюс consumer слушает order.events
        val location = LocationValue.createOrThrow(5, 5)
        val order = orderRepository.add(Order.create(UUID.randomUUID(), location, VolumeValue(5)))
        courierRepository.add(Courier.create("Courier", location))
        val consumer = newOrderEventsConsumer()

        // When — назначаем заказ
        assignOrderCommandHandler.handle(AssignOrderCommand.create().getOrNull()!!)

        // Then — в order.events появляется OrderAssignedIntegrationEvent с order_id
        val records = pollForRecords(consumer, order.id)
        val events = records.map { OrderAssignedIntegrationEvent.parseFrom(it.value()) }
        assertAll(
            { assertThat(events).describedAs("at least one event").isNotEmpty },
            {
                assertThat(events.map { it.orderId })
                    .describedAs("contains order_id")
                    .contains(order.id.toString())
            },
        )
    }

    @Test
    fun `when order is completed then OrderCompletedIntegrationEvent is published`() {
        // Given — заказ назначен курьеру, оба в одной локации, consumer слушает order.events
        val location = LocationValue.createOrThrow(5, 5)
        val order = orderRepository.add(Order.create(UUID.randomUUID(), location, VolumeValue(5)))
        val courier = courierRepository.add(Courier.create("Courier", location))
        assignOrderCommandHandler.handle(AssignOrderCommand.create().getOrNull()!!)
        val consumer = newOrderEventsConsumer()

        // When — завершаем заказ
        completeOrderCommandHandler.handle(
            CompleteOrderCommand.create(courierId = courier.id, orderId = order.id).getOrNull()!!,
        )

        // Then — в order.events появляется OrderCompletedIntegrationEvent с order_id
        val records = pollForRecords(consumer, order.id)
        val events = records.map { OrderCompletedIntegrationEvent.parseFrom(it.value()) }
        assertAll(
            { assertThat(events).describedAs("at least one event").isNotEmpty },
            {
                assertThat(events.map { it.orderId })
                    .describedAs("contains order_id")
                    .contains(order.id.toString())
            },
        )
    }

    private fun newOrderEventsConsumer(): KafkaConsumer<String, ByteArray> {
        val props = Properties()
        props["bootstrap.servers"] = bootstrapServers
        props["group.id"] = "test-consumer-${UUID.randomUUID()}"
        props["auto.offset.reset"] = "earliest"
        props["enable.auto.commit"] = "true"
        val consumer = KafkaConsumer(props, StringDeserializer(), ByteArrayDeserializer())
        consumer.subscribe(Collections.singletonList(topic))
        consumer.poll(Duration.ofSeconds(1))
        return consumer
    }

    private fun pollForRecords(
        consumer: KafkaConsumer<String, ByteArray>,
        expectedKey: UUID,
    ): List<ConsumerRecord<String, ByteArray>> {
        val collected = mutableListOf<ConsumerRecord<String, ByteArray>>()
        await()
            .atMost(20, SECONDS)
            .pollDelay(1, SECONDS)
            .pollInterval(1, SECONDS)
            .untilAsserted {
                consumer.poll(Duration.ofSeconds(1)).forEach { record ->
                    if (record.key() == expectedKey.toString()) collected.add(record)
                }
                assertThat(collected).isNotEmpty
            }
        return collected
    }
}
