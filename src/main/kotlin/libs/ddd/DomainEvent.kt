package libs.ddd

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.Instant
import java.util.UUID
import org.springframework.context.ApplicationEvent

abstract class DomainEvent : ApplicationEvent {
    val eventId: UUID = UUID.randomUUID()

    val occurredOnUtc: Instant = Instant.now()

    constructor(source: Any?) : super(source)

    // Fake Ctr for Jackson / JPA
    protected constructor() : super("default")

    @JsonIgnore
    override fun getSource(): Any? = super.getSource()
}
