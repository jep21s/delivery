package microarch.delivery.application

import microarch.delivery.core.domain.model.order.events.OrderAssignedDomainEvent
import microarch.delivery.core.domain.model.order.events.OrderCompletedDomainEvent
import microarch.delivery.core.ports.DomainEventProducer
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class DomainEventListener(
    private val producer: DomainEventProducer,
) {
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun onOrderAssigned(event: OrderAssignedDomainEvent) {
        producer.produce(event)
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    fun onOrderCompleted(event: OrderCompletedDomainEvent) {
        producer.produce(event)
    }
}
