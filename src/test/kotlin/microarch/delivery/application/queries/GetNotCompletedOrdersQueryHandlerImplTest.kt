package microarch.delivery.application.queries

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.domain.model.order.OrderStatus
import microarch.delivery.core.ports.order.OrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class GetNotCompletedOrdersQueryHandlerImplTest {
    @Test
    fun `handle asks for CREATED and ASSIGNED orders and maps them to dto`() {
        // Given — репозиторий отдаёт CREATED и ASSIGNED заказы
        val orderRepository = mockk<OrderRepository>()
        val handler = GetNotCompletedOrdersQueryHandlerImpl(orderRepository)

        val created = Order.create(UUID.randomUUID(), LocationValue.createOrThrow(1, 2), VolumeValue(1))
        val assigned = Order.create(UUID.randomUUID(), LocationValue.createOrThrow(3, 4), VolumeValue(1))
        assigned.assignOrder().getOrNull()!!
        every { orderRepository.getAllByStatusIn(setOf(OrderStatus.CREATED, OrderStatus.ASSIGNED)) } returns
            listOf(created, assigned)

        // When
        val result = handler.handle(GetNotCompletedOrdersQuery.create().getOrNull()!!)

        // Then
        val dtos = result.getOrNull()!!
        val byId = dtos.associateBy { it.id }
        assertAll(
            { assertThat(result.isRight()).describedAs("handle is right").isTrue() },
            { assertThat(dtos).describedAs("two orders").hasSize(2) },
            { assertThat(byId[created.id]?.location?.x).describedAs("created x").isEqualTo(1) },
            { assertThat(byId[created.id]?.location?.y).describedAs("created y").isEqualTo(2) },
            { assertThat(byId[assigned.id]?.location?.x).describedAs("assigned x").isEqualTo(3) },
            { assertThat(byId[assigned.id]?.location?.y).describedAs("assigned y").isEqualTo(4) },
        )
        verify(exactly = 1) { orderRepository.getAllByStatusIn(setOf(OrderStatus.CREATED, OrderStatus.ASSIGNED)) }
    }

    @Test
    fun `handle returns an empty list when the repository has no matching orders`() {
        // Given — репозиторий ничего не отдаёт
        val orderRepository = mockk<OrderRepository>()
        val handler = GetNotCompletedOrdersQueryHandlerImpl(orderRepository)

        every { orderRepository.getAllByStatusIn(any()) } returns emptyList()

        // When
        val result = handler.handle(GetNotCompletedOrdersQuery.create().getOrNull()!!)

        // Then
        assertThat(result.getOrNull()).describedAs("empty list").isEmpty()
        verify(exactly = 1) { orderRepository.getAllByStatusIn(any()) }
    }
}
