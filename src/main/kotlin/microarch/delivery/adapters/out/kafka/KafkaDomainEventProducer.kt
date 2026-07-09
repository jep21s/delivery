package microarch.delivery.adapters.out.kafka

import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutionException
import libs.ddd.DomainEvent
import microarch.delivery.core.domain.model.order.events.OrderAssignedDomainEvent
import microarch.delivery.core.domain.model.order.events.OrderCompletedDomainEvent
import microarch.delivery.core.ports.DomainEventProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import queues.order.events.OrderEventsProto.OrderAssignedIntegrationEvent
import queues.order.events.OrderEventsProto.OrderCompletedIntegrationEvent

@Component
class KafkaDomainEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, ByteArray>,
) : DomainEventProducer {
    @Value("\${app.kafka.order-events-topic}")
    private lateinit var topic: String

    override fun produce(event: DomainEvent) {
        try {
            when (event) {
                is OrderAssignedDomainEvent -> {
                    val proto =
                        OrderAssignedIntegrationEvent
                            .newBuilder()
                            .setOrderId(event.orderId.toString())
                            .build()
                    send(event.orderId.toString(), proto.toByteArray(), EventType.ORDER_ASSIGNED)
                }

                is OrderCompletedDomainEvent -> {
                    val proto =
                        OrderCompletedIntegrationEvent
                            .newBuilder()
                            .setOrderId(event.orderId.toString())
                            .build()
                    send(event.orderId.toString(), proto.toByteArray(), EventType.ORDER_COMPLETED)
                }

                else -> {
                    throw IllegalArgumentException("Unknown event type: ${event::class}")
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Kafka publish interrupted", e)
        } catch (e: ExecutionException) {
            throw RuntimeException("Kafka publish failed", e)
        }
    }

    private fun send(
        key: String,
        payload: ByteArray,
        eventType: EventType,
    ) {
        val record =
            ProducerRecord<String, ByteArray>(
                topic,
                null,
                key,
                payload,
                listOf(RecordHeader(EVENT_TYPE_HEADER, eventType.name.toByteArray(StandardCharsets.UTF_8))),
            )
        kafkaTemplate.send(record).get()
    }

    companion object {
        const val EVENT_TYPE_HEADER = "event_type"
    }
}
