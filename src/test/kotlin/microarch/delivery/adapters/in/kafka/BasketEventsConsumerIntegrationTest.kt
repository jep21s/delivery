package microarch.delivery.adapters.`in`.kafka

import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS
import microarch.delivery.BaseIntegrationTest
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.order.OrderStatus
import microarch.delivery.core.ports.order.OrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import queues.basket.events.BasketEventsProto

class BasketEventsConsumerIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String, ByteArray>

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Value("\${app.kafka.basket-events-topic}")
    private lateinit var topic: String

    @Test
    fun `consumed BasketConfirmedIntegrationEvent creates a new CREATED order`() {
        // Given — событие подтверждения корзины
        val basketId = UUID.randomUUID()
        val event =
            BasketEventsProto.BasketConfirmedIntegrationEvent
                .newBuilder()
                .setBasketId(basketId.toString())
                .setAddress(
                    BasketEventsProto.Address
                        .newBuilder()
                        .setCountry("Россия")
                        .setCity("Москва")
                        .setStreet("Тверская")
                        .setHouse("1")
                        .setApartment("42")
                        .build(),
                ).setVolume(7)
                .build()

        // When — публикуем protobuf-сообщение в топик
        kafkaTemplate.send(topic, basketId.toString(), event.toByteArray()).get(10, SECONDS)

        // Then — заказ асинхронно создаётся в БД
        await()
            .atMost(20, SECONDS)
            .pollDelay(1, SECONDS)
            .pollInterval(1, SECONDS)
            .untilAsserted {
                val order = orderRepository.getById(basketId)
                assertAll(
                    { assertThat(order).describedAs("order created").isNotNull },
                    { assertThat(order?.status).describedAs("status CREATED").isEqualTo(OrderStatus.CREATED) },
                    { assertThat(order?.volume).describedAs("volume").isEqualTo(VolumeValue(7)) },
                )
            }
    }
}
