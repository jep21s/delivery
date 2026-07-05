package microarch.delivery.application.commands

import arrow.core.Either
import arrow.core.raise.either
import libs.ddd.DomainEventPublisher
import libs.errs.LogicError
import microarch.delivery.core.domain.services.OrderDistributionService
import microarch.delivery.core.ports.courier.CourierRepository
import microarch.delivery.core.ports.order.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AssignOrderCommandHandlerImpl(
    private val orderRepository: OrderRepository,
    private val courierRepository: CourierRepository,
    private val orderDistributionService: OrderDistributionService,
    private val domainEventPublisher: DomainEventPublisher,
) : AssignOrderCommandHandler {
    @Transactional
    override fun handle(command: AssignOrderCommand): Either<LogicError, Unit> =
        either {
            val order = orderRepository.getFirstCreated() ?: return@either
            val couriers = courierRepository.getAll()
            val winner = orderDistributionService.distribute(order, couriers).bind()
            val savedOrder = orderRepository.update(order)
            val savedCourier = courierRepository.update(winner)
            domainEventPublisher.publish(listOf(savedOrder, savedCourier))
        }
}
