package microarch.delivery.core.domain.model.order.events

import java.util.UUID
import libs.ddd.DomainEvent
import microarch.delivery.core.domain.model.order.Order

class OrderAssignedDomainEvent(
    val orderId: UUID,
) : DomainEvent() {
    constructor(order: Order) : this(order.id)
}
