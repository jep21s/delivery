package libs.ddd

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient

@MappedSuperclass
abstract class Aggregate<TId : Comparable<TId>> protected constructor(
    id: TId? = null,
) : BaseEntity<TId>(id),
    AggregateRoot<TId> {
    @Transient
    private var _domainEvents: MutableList<DomainEvent> = mutableListOf()

    override fun getDomainEvents(): List<DomainEvent> = _domainEvents.toList()

    override fun clearDomainEvents() {
        _domainEvents.clear()
    }

    fun raiseDomainEvent(domainEvent: DomainEvent) {
        _domainEvents.add(domainEvent)
    }
}
