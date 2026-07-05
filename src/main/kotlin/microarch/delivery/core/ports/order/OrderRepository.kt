package microarch.delivery.core.ports.order

import java.util.UUID
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.domain.model.order.OrderStatus

interface OrderRepository {
    fun add(order: Order): Order

    fun update(order: Order): Order

    fun getById(id: UUID): Order?

    fun getFirstCreated(): Order?

    fun getAllByStatusIn(statuses: Set<OrderStatus>): List<Order>
}
