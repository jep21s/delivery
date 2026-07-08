package microarch.delivery.adapters.out.kafka

import java.util.concurrent.ExecutionException
import libs.ddd.DomainEvent
import microarch.delivery.core.domain.model.order.events.OrderAssignedDomainEvent
import microarch.delivery.core.domain.model.order.events.OrderCompletedDomainEvent
import microarch.delivery.core.ports.DomainEventProducer
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
                    kafkaTemplate.send(topic, event.orderId.toString(), proto.toByteArray()).get()
                }

                is OrderCompletedDomainEvent -> {
                    val proto =
                        OrderCompletedIntegrationEvent
                            .newBuilder()
                            .setOrderId(event.orderId.toString())
                            .build()
                    kafkaTemplate.send(topic, event.orderId.toString(), proto.toByteArray()).get()
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
}
