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
import microarch.delivery.core.domain.model.assignment.Assignment
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.domain.model.order.OrderStatus
import microarch.delivery.core.ports.courier.CourierRepository
import microarch.delivery.core.ports.order.OrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class CompleteOrderCommandHandlerImplTest {
    @Test
    fun `handle completes the assignment and the order when the courier is at the delivery location`() {
        // Given — назначенный заказ и курьер в точке доставки
        val orderRepository = mockk<OrderRepository>()
        val courierRepository = mockk<CourierRepository>()
        val publisher = mockk<DomainEventPublisher>()
        val handler = CompleteOrderCommandHandlerImpl(orderRepository, courierRepository, publisher)

        val location = LocationValue.createOrThrow(3, 3)
        val order = Order.create(UUID.randomUUID(), location, VolumeValue(1))
        order.assignOrder().getOrNull()!!
        val courier = Courier.create("courier", location)
        courier.takeOrder(Courier.NewOrder(order.id, order.volume, order.location)).getOrNull()!!

        every { courierRepository.getById(courier.id) } returns courier
        every { orderRepository.getById(order.id) } returns order
        every { courierRepository.update(any()) } answers { firstArg() }
        every { orderRepository.update(any()) } answers { firstArg() }
        every { publisher.publish(any()) } just Runs

        val command = CompleteOrderCommand.create(courier.id, order.id).getOrNull()!!

        // When
        val result = handler.handle(command)

        // Then
        assertAll(
            { assertThat(result.isRight()).describedAs("handle is right").isTrue() },
            {
                assertThat(courier.assignments.first().status)
                    .describedAs("assignment COMPLETED")
                    .isEqualTo(Assignment.Status.COMPLETED)
            },
            { assertThat(order.status).describedAs("order COMPLETED").isEqualTo(OrderStatus.COMPLETED) },
        )
        verify(exactly = 1) { courierRepository.update(courier) }
        verify(exactly = 1) { orderRepository.update(order) }
        verify { publisher.publish(any()) }
    }

    @Test
    fun `handle fails when the courier is not found`() {
        // Given — курьера нет
        val orderRepository = mockk<OrderRepository>()
        val courierRepository = mockk<CourierRepository>()
        val publisher = mockk<DomainEventPublisher>()
        val handler = CompleteOrderCommandHandlerImpl(orderRepository, courierRepository, publisher)

        val order = Order.create(UUID.randomUUID(), LocationValue.createOrThrow(3, 3), VolumeValue(1))
        order.assignOrder().getOrNull()!!
        every { courierRepository.getById(any()) } returns null
        val command = CompleteOrderCommand.create(UUID.randomUUID(), order.id).getOrNull()!!

        // When
        val result = handler.handle(command)

        // Then
        assertAll(
            { assertThat(result.isLeft()).describedAs("handle is left").isTrue() },
            { assertThat(result.leftOrNull()?.code).describedAs("error code").isEqualTo("record.not.found") },
        )
        verify(exactly = 0) { orderRepository.update(any()) }
        verify(exactly = 0) { courierRepository.update(any()) }
    }

    @Test
    fun `handle fails when the order is not found`() {
        // Given — заказ не найден
        val orderRepository = mockk<OrderRepository>()
        val courierRepository = mockk<CourierRepository>()
        val publisher = mockk<DomainEventPublisher>()
        val handler = CompleteOrderCommandHandlerImpl(orderRepository, courierRepository, publisher)

        val courier = Courier.create("courier", LocationValue.createOrThrow(3, 3))
        every { courierRepository.getById(courier.id) } returns courier
        every { orderRepository.getById(any()) } returns null
        val command = CompleteOrderCommand.create(courier.id, UUID.randomUUID()).getOrNull()!!

        // When
        val result = handler.handle(command)

        // Then
        assertAll(
            { assertThat(result.isLeft()).describedAs("handle is left").isTrue() },
            { assertThat(result.leftOrNull()?.code).describedAs("error code").isEqualTo("record.not.found") },
        )
        verify(exactly = 0) { orderRepository.update(any()) }
        verify(exactly = 0) { courierRepository.update(any()) }
    }

    @Test
    fun `handle fails when the courier has no assignment for the order`() {
        // Given — курьер не брал этот заказ
        val orderRepository = mockk<OrderRepository>()
        val courierRepository = mockk<CourierRepository>()
        val publisher = mockk<DomainEventPublisher>()
        val handler = CompleteOrderCommandHandlerImpl(orderRepository, courierRepository, publisher)

        val order = Order.create(UUID.randomUUID(), LocationValue.createOrThrow(3, 3), VolumeValue(1))
        order.assignOrder().getOrNull()!!
        val courier = Courier.create("courier", LocationValue.createOrThrow(3, 3))
        every { courierRepository.getById(courier.id) } returns courier
        every { orderRepository.getById(order.id) } returns order
        val command = CompleteOrderCommand.create(courier.id, order.id).getOrNull()!!

        // When
        val result = handler.handle(command)

        // Then
        assertAll(
            { assertThat(result.isLeft()).describedAs("handle is left").isTrue() },
            { assertThat(result.leftOrNull()?.code).describedAs("error code").isEqualTo("404") },
            { assertThat(result.leftOrNull()?.message).describedAs("error message").contains("no assignment") },
        )
        verify(exactly = 0) { orderRepository.update(any()) }
        verify(exactly = 0) { courierRepository.update(any()) }
    }

    @Test
    fun `handle fails when the courier is not at the delivery location`() {
        // Given — курьер взял заказ, но находится в другой точке
        val orderRepository = mockk<OrderRepository>()
        val courierRepository = mockk<CourierRepository>()
        val publisher = mockk<DomainEventPublisher>()
        val handler = CompleteOrderCommandHandlerImpl(orderRepository, courierRepository, publisher)

        val orderLocation = LocationValue.createOrThrow(3, 3)
        val order = Order.create(UUID.randomUUID(), orderLocation, VolumeValue(1))
        order.assignOrder().getOrNull()!!
        val courier = Courier.create("courier", LocationValue.createOrThrow(5, 5))
        courier.takeOrder(Courier.NewOrder(order.id, order.volume, order.location)).getOrNull()!!

        every { courierRepository.getById(courier.id) } returns courier
        every { orderRepository.getById(order.id) } returns order
        val command = CompleteOrderCommand.create(courier.id, order.id).getOrNull()!!

        // When
        val result = handler.handle(command)

        // Then
        assertAll(
            { assertThat(result.isLeft()).describedAs("handle is left").isTrue() },
            { assertThat(result.leftOrNull()?.message).describedAs("error message").contains("not close enough") },
            { assertThat(order.status).describedAs("order stays ASSIGNED").isEqualTo(OrderStatus.ASSIGNED) },
        )
        verify(exactly = 0) { orderRepository.update(any()) }
        verify(exactly = 0) { courierRepository.update(any()) }
    }
}
