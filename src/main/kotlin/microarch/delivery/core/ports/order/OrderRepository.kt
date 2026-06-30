package microarch.delivery.core.ports.order

import java.util.UUID
import microarch.delivery.core.domain.model.order.Order

interface OrderRepository {
    fun add(order: Order): Order

    fun update(order: Order): Order

    fun getById(id: UUID): Order?

    fun getFirstCreated(): Order?

    fun getAllAssigned(): List<Order>
}
