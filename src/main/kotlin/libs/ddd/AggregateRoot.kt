package libs.ddd

interface AggregateRoot<ID> {
    val id: ID?

    fun getDomainEvents(): List<DomainEvent>

    fun clearDomainEvents()
}
