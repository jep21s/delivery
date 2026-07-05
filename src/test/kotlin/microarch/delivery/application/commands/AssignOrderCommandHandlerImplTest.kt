package microarch.delivery.application.commands

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import libs.ddd.DomainEventPublisher
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.domain.model.order.OrderStatus
import microarch.delivery.core.domain.services.OrderDistributionServiceImpl
import microarch.delivery.core.ports.courier.CourierRepository
import microarch.delivery.core.ports.order.OrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class AssignOrderCommandHandlerImplTest {
    @Test
    fun `handle assigns a created order to the closest courier and persists both`() {
        // Given — один CREATED заказ и один курьер рядом
        val orderRepository = mockk<OrderRepository>()
        val courierRepository = mockk<CourierRepository>()
        val publisher = mockk<DomainEventPublisher>()
        val handler =
            AssignOrderCommandHandlerImpl(
                orderRepository,
                courierRepository,
                OrderDistributionServiceImpl(),
                publisher,
            )

        val order = Order.create(UUID.randomUUID(), LocationValue.createOrThrow(5, 5), VolumeValue(1))
        val courier = Courier.create("courier", LocationValue.createOrThrow(5, 6))
        every { orderRepository.getFirstCreated() } returns order
        every { courierRepository.getAll() } returns listOf(courier)
        every { orderRepository.update(any()) } answers { firstArg() }
        every { courierRepository.update(any()) } answers { firstArg() }
        every { publisher.publish(any()) } just Runs

        // When
        val result = handler.handle(AssignOrderCommand.create().getOrNull()!!)

        // Then
        assertAll(
            { assertThat(result.isRight()).describedAs("handle is right").isTrue() },
            { assertThat(order.status).describedAs("order ASSIGNED").isEqualTo(OrderStatus.ASSIGNED) },
            { assertThat(courier.assignments).describedAs("courier got assignment").hasSize(1) },
            {
                assertThat(courier.assignments.first().orderId)
                    .describedAs("assignment orderId")
                    .isEqualTo(order.id)
            },
        )
        verify(exactly = 1) { orderRepository.update(order) }
        verify(exactly = 1) { courierRepository.update(courier) }
        verify { publisher.publish(any()) }
    }

    @Test
    fun `handle is a success no-op when there is no created order`() {
        // Given — нет CREATED заказов
        val orderRepository = mockk<OrderRepository>()
        val courierRepository = mockk<CourierRepository>()
        val publisher = mockk<DomainEventPublisher>()
        val handler =
            AssignOrderCommandHandlerImpl(
                orderRepository,
                courierRepository,
                OrderDistributionServiceImpl(),
                publisher,
            )

        every { orderRepository.getFirstCreated() } returns null
        every { courierRepository.getAll() } returns listOf(Courier.create("courier", LocationValue.createOrThrow(1, 1)))
        every { publisher.publish(any()) } just Runs

        // When
        val result = handler.handle(AssignOrderCommand.create().getOrNull()!!)

        // Then
        assertThat(result.isRight()).describedAs("handle is right (no-op)").isTrue()
        verify(exactly = 0) { orderRepository.update(any()) }
        verify(exactly = 0) { courierRepository.update(any()) }
        verify(exactly = 0) { publisher.publish(any()) }
    }

    @Test
    fun `handle fails when there are no available couriers`() {
        // Given — есть CREATED заказ, но нет ни одного курьера
        val orderRepository = mockk<OrderRepository>()
        val courierRepository = mockk<CourierRepository>()
        val publisher = mockk<DomainEventPublisher>()
        val handler =
            AssignOrderCommandHandlerImpl(
                orderRepository,
                courierRepository,
                OrderDistributionServiceImpl(),
                publisher,
            )

        val order = Order.create(UUID.randomUUID(), LocationValue.createOrThrow(5, 5), VolumeValue(1))
        every { orderRepository.getFirstCreated() } returns order
        every { courierRepository.getAll() } returns emptyList()
        every { orderRepository.update(any()) } answers { firstArg() }
        every { publisher.publish(any()) } just Runs

        // When
        val result = handler.handle(AssignOrderCommand.create().getOrNull()!!)

        // Then
        assertAll(
            { assertThat(result.isLeft()).describedAs("handle is left").isTrue() },
            { assertThat(result.leftOrNull()?.code).describedAs("error code").isEqualTo("400") },
            { assertThat(order.status).describedAs("order stays CREATED").isEqualTo(OrderStatus.CREATED) },
        )
        verify(exactly = 0) { orderRepository.update(any()) }
        verify(exactly = 0) { courierRepository.update(any()) }
        verify(exactly = 0) { publisher.publish(any()) }
    }
}
