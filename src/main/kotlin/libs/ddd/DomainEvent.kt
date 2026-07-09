package libs.ddd

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.Instant
import java.util.UUID

abstract class DomainEvent {
    @JsonIgnore
    val eventId: UUID = UUID.randomUUID()

    @JsonIgnore
    val occurredOnUtc: Instant = Instant.now()

    protected constructor()

    constructor(source: Any?)
}
