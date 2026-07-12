package microarch.delivery.adapters.out.postgres.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import libs.ddd.Aggregate
import libs.ddd.DomainEventPublisher
import org.springframework.stereotype.Component

@Component
class OutboxDomainEventPublisher(
    private val jpa: OutboxJpaRepository,
    private val objectMapper: ObjectMapper,
) : DomainEventPublisher {
    override fun publish(aggregates: Iterable<Aggregate<*>>) {
        for (aggregate in aggregates) {
            aggregate.getDomainEvents().forEach { domainEvent ->
                try {
                    val payload = objectMapper.writeValueAsString(domainEvent)
                    val outboxMessage =
                        OutboxMessage(
                            id = domainEvent.eventId,
                            eventType = domainEvent::class.java.name,
                            aggregateId = aggregate.id.toString(),
                            aggregateType = aggregate::class.java.simpleName,
                            payload = payload,
                            occurredOnUtc = domainEvent.occurredOnUtc,
                        )
                    jpa.save(outboxMessage)
                } catch (e: Exception) {
                    throw RuntimeException("Failed to serialize domainEvent for Outbox", e)
                }
            }
            aggregate.clearDomainEvents()
        }
    }
}
