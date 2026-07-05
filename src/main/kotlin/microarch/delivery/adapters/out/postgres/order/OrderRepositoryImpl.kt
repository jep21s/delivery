package microarch.delivery.adapters.out.postgres.order

import java.util.UUID
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.domain.model.order.OrderStatus
import microarch.delivery.core.ports.order.OrderRepository
import org.springframework.stereotype.Repository

@Repository
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository {
    override fun add(order: Order): Order = orderJpaRepository.save(order)

    override fun update(order: Order): Order = orderJpaRepository.save(order)

    override fun getById(id: UUID): Order? = orderJpaRepository.findById(id).orElse(null)

    override fun getFirstCreated(): Order? = orderJpaRepository.findFirstByStatus(OrderStatus.CREATED)

    override fun getAllAssigned(): List<Order> = orderJpaRepository.findAllByStatus(OrderStatus.ASSIGNED)
}
