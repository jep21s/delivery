package microarch.delivery.adapters.`in`.kafka

import arrow.core.left
import arrow.core.right
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID
import libs.errs.LogicError
import microarch.delivery.application.commands.CreateOrderCommand
import microarch.delivery.application.commands.CreateOrderCommandHandler
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import queues.basket.events.BasketEventsProto

class BasketEventsConsumerTest {
    @Test
    fun `listen parses BasketConfirmedIntegrationEvent and delegates a CreateOrderCommand to the handler`() {
        // Given — реальное protobuf-сообщение
        val handler = mockk<CreateOrderCommandHandler>()
        val captured = slot<CreateOrderCommand>()
        every { handler.handle(capture(captured)) } returns Unit.right()
        val consumer = BasketEventsConsumer(handler)

        val basketId = UUID.randomUUID()
        val event =
            BasketEventsProto.BasketConfirmedIntegrationEvent
                .newBuilder()
                .setBasketId(basketId.toString())
                .setAddress(address())
                .addItems(item())
                .setVolume(7)
                .build()

        // When
        consumer.listen(event.toByteArray())

        // Then
        val command = captured.captured
        assertAll(
            { assertThat(command.orderId).describedAs("orderId <- basketId").isEqualTo(basketId) },
            { assertThat(command.country).describedAs("country").isEqualTo("Россия") },
            { assertThat(command.city).describedAs("city").isEqualTo("Москва") },
            { assertThat(command.street).describedAs("street").isEqualTo("Тверская") },
            { assertThat(command.house).describedAs("house").isEqualTo("1") },
            { assertThat(command.apartment).describedAs("apartment").isEqualTo("42") },
            { assertThat(command.volume).describedAs("volume").isEqualTo(7) },
        )
        verify(exactly = 1) { handler.handle(any()) }
    }

    @Test
    fun `listen throws RuntimeException when the handler returns Left`() {
        // Given — handler возвращает ошибку
        val handler = mockk<CreateOrderCommandHandler>()
        val failure = LogicError.of("test.code", "boom")
        every { handler.handle(any()) } returns failure.left()
        val consumer = BasketEventsConsumer(handler)

        val event = validEvent()

        // When / Then
        assertThatThrownBy { consumer.listen(event.toByteArray()) }
            .isInstanceOf(RuntimeException::class.java)
            .hasMessageContaining("Failed to handle command")
            .hasMessageContaining("test.code")
    }

    @Test
    fun `listen throws RuntimeException when the protobuf bytes are invalid`() {
        // Given — мусор вместо protobuf
        val handler = mockk<CreateOrderCommandHandler>()
        every { handler.handle(any()) } returns Unit.right()
        val consumer = BasketEventsConsumer(handler)

        // When / Then
        assertThatThrownBy { consumer.listen(byteArrayOf(0x00, 0x01, 0x02, 0x03)) }
            .isInstanceOf(RuntimeException::class.java)
        verify(exactly = 0) { handler.handle(any()) }
    }

    private fun address(): BasketEventsProto.Address =
        BasketEventsProto.Address
            .newBuilder()
            .setCountry("Россия")
            .setCity("Москва")
            .setStreet("Тверская")
            .setHouse("1")
            .setApartment("42")
            .build()

    private fun item(): BasketEventsProto.Item =
        BasketEventsProto.Item
            .newBuilder()
            .setId(UUID.randomUUID().toString())
            .setGoodId(UUID.randomUUID().toString())
            .setTitle("Milk")
            .setPrice(99.0)
            .setQuantity(2)
            .build()

    private fun validEvent(): BasketEventsProto.BasketConfirmedIntegrationEvent =
        BasketEventsProto.BasketConfirmedIntegrationEvent
            .newBuilder()
            .setBasketId(UUID.randomUUID().toString())
            .setAddress(address())
            .setVolume(3)
            .build()
}
