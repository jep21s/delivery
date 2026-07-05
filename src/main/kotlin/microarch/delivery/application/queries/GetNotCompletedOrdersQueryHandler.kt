package microarch.delivery.application.queries

import arrow.core.Either
import libs.errs.LogicError
import microarch.delivery.application.queries.dto.OrderDto

interface GetNotCompletedOrdersQueryHandler {
    fun handle(query: GetNotCompletedOrdersQuery): Either<LogicError, List<OrderDto>>
}
