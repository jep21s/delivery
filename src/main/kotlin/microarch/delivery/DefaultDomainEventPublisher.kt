package microarch.delivery

import libs.ddd.Aggregate
import libs.ddd.DomainEventPublisher
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class DefaultDomainEventPublisher(
    private val publisher: ApplicationEventPublisher,
) : DomainEventPublisher {
    override fun publish(aggregates: Iterable<Aggregate<*>>) {
        for (aggregate in aggregates) {
            for (event in aggregate.getDomainEvents()) {
                publisher.publishEvent(event)
            }
        }
    }
}
