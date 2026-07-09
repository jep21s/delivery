package microarch.delivery.core.ports

import libs.ddd.DomainEvent

interface DomainEventProducer {
    fun produce(event: DomainEvent)
}
