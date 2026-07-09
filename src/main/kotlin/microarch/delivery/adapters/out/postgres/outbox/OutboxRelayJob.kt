package microarch.delivery.adapters.out.postgres.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import libs.ddd.DomainEvent
import microarch.delivery.core.ports.DomainEventProducer
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OutboxRelayJob(
    private val jpa: OutboxJpaRepository,
    private val objectMapper: ObjectMapper,
    private val producer: DomainEventProducer,
) {
    @Scheduled(fixedDelay = 1_000)
    fun run() {
        val messages = jpa.findUnprocessedMessages()
        for (message in messages) {
            try {
                val eventClass = Class.forName(message.eventType)
                val eventObject = objectMapper.readValue(message.payload, eventClass)
                require(eventObject is DomainEvent) { "Invalid outbox message type: ${message.eventType}" }
                producer.produce(eventObject)
                message.markAsProcessed()
                jpa.save(message)
            } catch (e: Exception) {
                log.error(e) { "Failed to publish outbox message ${message.id}" }
            }
        }
    }

    companion object {
        private val log = KotlinLogging.logger {}
    }
}
