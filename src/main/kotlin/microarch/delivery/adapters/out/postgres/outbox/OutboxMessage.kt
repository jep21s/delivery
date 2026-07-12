package microarch.delivery.adapters.out.postgres.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "outbox")
class OutboxMessage {
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID? = null

    @Column(name = "event_type", nullable = false)
    var eventType: String? = null

    @Column(name = "aggregate_id", nullable = false)
    var aggregateId: String? = null

    @Column(name = "aggregate_type", nullable = false)
    var aggregateType: String? = null

    @Column(name = "payload", nullable = false, columnDefinition = "text")
    var payload: String? = null

    @Column(name = "occurred_on_utc", nullable = false)
    var occurredOnUtc: Instant? = null

    @Column(name = "processed_on_utc")
    var processedOnUtc: Instant? = null

    protected constructor()

    constructor(
        id: UUID,
        eventType: String,
        aggregateId: String,
        aggregateType: String,
        payload: String,
        occurredOnUtc: Instant,
    ) {
        require(eventType.isNotEmpty()) { "eventType must not be null or empty" }
        require(aggregateId.isNotEmpty()) { "aggregateId must not be null or empty" }
        require(aggregateType.isNotEmpty()) { "aggregateType must not be null or empty" }
        require(payload.isNotEmpty()) { "payload must not be null or empty" }

        this.id = id
        this.eventType = eventType
        this.aggregateId = aggregateId
        this.aggregateType = aggregateType
        this.payload = payload
        this.occurredOnUtc = occurredOnUtc
    }

    fun markAsProcessed() {
        this.processedOnUtc = Instant.now()
    }
}
