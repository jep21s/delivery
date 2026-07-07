package microarch.delivery.adapters.out.postgres.order

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.util.UUID
import microarch.delivery.BaseIntegrationTest
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.domain.model.order.OrderStatus
import microarch.delivery.core.ports.order.OrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class OrderRepositoryIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var orderRepository: OrderRepository

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Test
    fun `add persists an order and getById reads it back`() {
        // Given — новый заказ в статусе CREATED
        val id = UUID.randomUUID()
        val order = Order.create(id, LocationValue.createOrThrow(4, 6), VolumeValue(3))

        // When
        orderRepository.add(order)
        flushAndClear()
        val found = orderRepository.getById(id)

        // Then
        assertAll(
            { assertThat(found).describedAs("order found").isNotNull },
            { assertThat(found?.id).describedAs("id").isEqualTo(id) },
            { assertThat(found?.location).describedAs("location").isEqualTo(LocationValue.createOrThrow(4, 6)) },
            { assertThat(found?.volume).describedAs("volume").isEqualTo(VolumeValue(3)) },
            { assertThat(found?.status).describedAs("status CREATED").isEqualTo(OrderStatus.CREATED) },
        )
    }

    @Test
    fun `update persists a transition to ASSIGNED`() {
        // Given — сохранённый заказ
        val order =
            orderRepository.add(
                Order.create(UUID.randomUUID(), LocationValue.createOrThrow(1, 1), VolumeValue(1)),
            )
        flushAndClear()

        // When — переход CREATED -> ASSIGNED и обновление
        order.assignOrder().getOrNull()!!
        orderRepository.update(order)
        flushAndClear()
        val found = orderRepository.getById(order.id)

        // Then
        assertThat(found?.status).describedAs("status ASSIGNED").isEqualTo(OrderStatus.ASSIGNED)
    }

    @Test
    fun `getFirstCreated returns a single created order`() {
        // Given — один заказ в статусе CREATED
        val created =
            orderRepository.add(
                Order.create(UUID.randomUUID(), LocationValue.createOrThrow(2, 2), VolumeValue(1)),
            )
        flushAndClear()

        // When
        val first = orderRepository.getFirstCreated()

        // Then
        assertThat(first?.id).describedAs("first created").isEqualTo(created.id)
    }

    @Test
    fun `getFirstCreated returns null when there are no created orders`() {
        // Given — только назначенные заказы
        val order =
            orderRepository.add(
                Order.create(UUID.randomUUID(), LocationValue.createOrThrow(2, 2), VolumeValue(1)),
            )
        order.assignOrder().getOrNull()!!
        orderRepository.update(order)
        flushAndClear()

        // When
        val first = orderRepository.getFirstCreated()

        // Then
        assertThat(first).describedAs("no created orders").isNull()
    }

    @Test
    fun `getAllByStatusIn returns only orders with matching statuses`() {
        // Given — 2 назначенных, 1 новый, 1 выполненный
        val assigned1 = orderRepository.add(orderInStatus(OrderStatus.ASSIGNED))
        val assigned2 = orderRepository.add(orderInStatus(OrderStatus.ASSIGNED))
        val created = orderRepository.add(orderInStatus(OrderStatus.CREATED))
        orderRepository.add(orderInStatus(OrderStatus.COMPLETED))
        flushAndClear()

        // When
        val result = orderRepository.getAllByStatusIn(setOf(OrderStatus.CREATED, OrderStatus.ASSIGNED))

        // Then
        val ids = result.map { it.id }
        assertAll(
            { assertThat(result).describedAs("three not-completed").hasSize(3) },
            { assertThat(ids).describedAs("contains assigned1").contains(assigned1.id) },
            { assertThat(ids).describedAs("contains assigned2").contains(assigned2.id) },
            { assertThat(ids).describedAs("contains created").contains(created.id) },
            {
                assertThat(result.map { it.status })
                    .describedAs("only CREATED or ASSIGNED")
                    .containsOnly(OrderStatus.CREATED, OrderStatus.ASSIGNED)
            },
        )
    }

    @Test
    fun `getById returns null for unknown id`() {
        // When
        val found = orderRepository.getById(UUID.randomUUID())

        // Then
        assertThat(found).isNull()
    }

    /**
     * Создаёт [Order] в требуемом статусе, последовательно применяя
     * доменные переходы из начального статуса CREATED.
     */
    private fun orderInStatus(status: OrderStatus): Order {
        val current = Order.create(UUID.randomUUID(), LocationValue.createOrThrow(1, 1), VolumeValue(1))
        if (status == OrderStatus.CREATED) return current
        current.assignOrder().getOrNull()!!
        if (status == OrderStatus.ASSIGNED) return current
        current.completeOrder().getOrNull()!!
        return current
    }

    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }
}
