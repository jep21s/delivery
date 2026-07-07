package microarch.delivery.application.commands

import arrow.core.right
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.UUID
import libs.ddd.DomainEventPublisher
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.order.OrderStatus
import microarch.delivery.core.ports.geo.GeoClient
import microarch.delivery.core.ports.order.OrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class CreateOrderCommandHandlerImplTest {
    @Test
    fun `handle creates a CREATED order with given id and volume, location from Geo, and publishes event`() {
        // Given
        val orderRepository = mockk<OrderRepository>()
        val publisher = mockk<DomainEventPublisher>()
        val geoClient = mockk<GeoClient>()
        val fixedLocation = LocationValue.createOrThrow(7, 3)
        every {
            geoClient.getLocation(
                country = any(),
                city = any(),
                street = any(),
                house = any(),
                apartment = any(),
            )
        } returns fixedLocation.right()
        val handler = CreateOrderCommandHandlerImpl(orderRepository, geoClient, publisher)

        val added = slot<microarch.delivery.core.domain.model.order.Order>()
        every { orderRepository.add(capture(added)) } answers { added.captured }
        every { publisher.publish(any()) } just Runs

        val orderId = UUID.randomUUID()
        val command =
            CreateOrderCommand
                .create(
                    orderId = orderId,
                    country = "Россия",
                    city = "Москва",
                    street = "Тверская",
                    house = "1",
                    apartment = "2",
                    volume = 5,
                ).getOrNull()!!

        // When
        val result = handler.handle(command)

        // Then
        val saved = added.captured
        assertAll(
            { assertThat(result.isRight()).describedAs("handle is right").isTrue() },
            { assertThat(saved.id).describedAs("id").isEqualTo(orderId) },
            { assertThat(saved.status).describedAs("status CREATED").isEqualTo(OrderStatus.CREATED) },
            { assertThat(saved.volume).describedAs("volume").isEqualTo(VolumeValue(5)) },
            { assertThat(saved.location).describedAs("location from Geo").isEqualTo(fixedLocation) },
        )
        verify(exactly = 1) { geoClient.getLocation(any(), any(), any(), any(), any()) }
        verify(exactly = 1) { orderRepository.add(any()) }
        verify { publisher.publish(any()) }
    }
}
