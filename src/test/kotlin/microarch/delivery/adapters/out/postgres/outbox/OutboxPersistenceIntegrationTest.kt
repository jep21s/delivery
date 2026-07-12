package microarch.delivery.adapters.out.postgres.outbox

import java.util.UUID
import microarch.delivery.BaseIntegrationTest
import microarch.delivery.application.commands.AssignOrderCommand
import microarch.delivery.application.commands.AssignOrderCommandHandler
import microarch.delivery.core.domain.model.LocationValue
import microarch.delivery.core.domain.model.VolumeValue
import microarch.delivery.core.domain.model.courier.Courier
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.domain.model.order.events.OrderAssignedDomainEvent
import microarch.delivery.core.ports.courier.CourierRepository
import microarch.delivery.core.ports.order.OrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class OutboxPersistenceIntegrationTest : BaseIntegrationTest() {
    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var courierRepository: CourierRepository

    @Autowired
    private lateinit var assignOrderCommandHandler: AssignOrderCommandHandler

    @Autowired
    private lateinit var outboxJpaRepository: OutboxJpaRepository

    @Test
    fun `when order is assigned then domain event is persisted in outbox table`() {
        val location = LocationValue.createOrThrow(5, 5)
        val order = orderRepository.add(Order.create(UUID.randomUUID(), location, VolumeValue(5)))
        courierRepository.add(Courier.create("Courier", location))

        assignOrderCommandHandler.handle(AssignOrderCommand.create().getOrNull()!!)

        val unprocessed = outboxJpaRepository.findUnprocessedMessages()
        val assignedEvent =
            unprocessed.find {
                it.eventType == OrderAssignedDomainEvent::class.java.name &&
                    it.aggregateId == order.id.toString()
            }

        assertThat(assignedEvent).describedAs("outbox row for OrderAssignedDomainEvent").isNotNull
        assignedEvent!!.let {
            assertThat(it.aggregateType).isEqualTo("Order")
            assertThat(it.payload).contains("\"orderId\"")
            assertThat(it.occurredOnUtc).isNotNull
            assertThat(it.processedOnUtc).isNull()
        }
    }
}
