package microarch.delivery.adapters.out.postgres.order

import java.util.UUID
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.domain.model.order.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository

interface OrderJpaRepository : JpaRepository<Order, UUID> {
    fun findFirstByStatus(status: OrderStatus): Order?

    fun findAllByStatus(status: OrderStatus): List<Order>
}
