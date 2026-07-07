package microarch.delivery.application.queries

import arrow.core.Either
import arrow.core.right
import libs.errs.LogicError
import microarch.delivery.application.queries.dto.LocationDto
import microarch.delivery.application.queries.dto.OrderDto
import microarch.delivery.core.domain.model.order.Order
import microarch.delivery.core.domain.model.order.OrderStatus
import microarch.delivery.core.ports.order.OrderRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetNotCompletedOrdersQueryHandlerImpl(
    private val orderRepository: OrderRepository,
) : GetNotCompletedOrdersQueryHandler {
    @Transactional(readOnly = true)
    override fun handle(query: GetNotCompletedOrdersQuery): Either<LogicError, List<OrderDto>> =
        orderRepository
            .getAllByStatusIn(setOf(OrderStatus.CREATED, OrderStatus.ASSIGNED))
            .map { it.toDto() }
            .right()

    private fun Order.toDto() =
        OrderDto(
            id = id,
            location = LocationDto(location.x, location.y),
        )
}
