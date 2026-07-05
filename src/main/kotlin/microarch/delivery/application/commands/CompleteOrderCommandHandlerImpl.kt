package microarch.delivery.application.commands

import arrow.core.Either
import arrow.core.raise.either
import libs.ddd.DomainEventPublisher
import libs.errs.GeneralErrors
import libs.errs.LogicError
import microarch.delivery.core.ports.courier.CourierRepository
import microarch.delivery.core.ports.order.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompleteOrderCommandHandlerImpl(
    private val orderRepository: OrderRepository,
    private val courierRepository: CourierRepository,
    private val domainEventPublisher: DomainEventPublisher,
) : CompleteOrderCommandHandler {
    @Transactional
    override fun handle(command: CompleteOrderCommand): Either<LogicError, Unit> =
        either {
            val courier =
                courierRepository.getById(command.courierId)
                    ?: raise(GeneralErrors.notFound("courier", command.courierId))
            val order =
                orderRepository.getById(command.orderId)
                    ?: raise(GeneralErrors.notFound("order", command.orderId))
            val assignment =
                courier.assignments.find { it.orderId == command.orderId }
                    ?: raise(
                        LogicError.of(
                            "404",
                            "Courier ${courier.id} has no assignment for order ${command.orderId}",
                        ),
                    )
            courier.completeAssignment(assignment.id).bind()
            order.completeOrder().bind()
            val savedCourier = courierRepository.update(courier)
            val savedOrder = orderRepository.update(order)
            domainEventPublisher.publish(listOf(savedCourier, savedOrder))
        }
}
