package microarch.delivery.core.domain.model.order.events

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID
import libs.ddd.DomainEvent
import microarch.delivery.core.domain.model.order.Order

class OrderAssignedDomainEvent
    @JsonCreator
    constructor(
        @JsonProperty("orderId") val orderId: UUID,
    ) : DomainEvent() {
        constructor(order: Order) : this(order.id)
    }
