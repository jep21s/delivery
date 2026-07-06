package libs.ddd

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient

@MappedSuperclass
abstract class Aggregate<TId : Comparable<TId>> protected constructor(
    id: TId,
) : BaseEntity<TId>(id),
    AggregateRoot<TId> {
    @Transient
    private var _domainEvents: MutableList<DomainEvent>? = mutableListOf()

    override fun getDomainEvents(): List<DomainEvent> = _domainEvents?.toList() ?: emptyList()

    override fun clearDomainEvents() {
        _domainEvents?.clear()
    }

    fun raiseDomainEvent(domainEvent: DomainEvent) {
        if (_domainEvents == null) _domainEvents = mutableListOf()
        _domainEvents!!.add(domainEvent)
    }
}
