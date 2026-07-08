package microarch.delivery.adapters.`in`.kafka

import arrow.core.getOrElse
import com.google.protobuf.InvalidProtocolBufferException
import java.util.UUID
import microarch.delivery.application.commands.CreateOrderCommand
import microarch.delivery.application.commands.CreateOrderCommandHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service
import queues.basket.events.BasketEventsProto

@Service
class BasketEventsConsumer(
    private val createOrderCommandHandler: CreateOrderCommandHandler,
) {
    @KafkaListener(topics = ["\${app.kafka.basket-events-topic}"])
    fun listen(message: ByteArray) {
        try {
            val event = BasketEventsProto.BasketConfirmedIntegrationEvent.parseFrom(message)

            val command =
                CreateOrderCommand
                    .create(
                        orderId = UUID.fromString(event.basketId),
                        country = event.address.country,
                        city = event.address.city,
                        street = event.address.street,
                        house = event.address.house,
                        apartment = event.address.apartment,
                        volume = event.volume,
                    ).getOrElse { error -> throw RuntimeException("Invalid command: $error") }

            createOrderCommandHandler
                .handle(command)
                .getOrElse { error -> throw RuntimeException("Failed to handle command: $error") }
        } catch (ex: InvalidProtocolBufferException) {
            throw RuntimeException("Failed to parse protobuf message", ex)
        }
    }
}
