package microarch.delivery.application.queries

import arrow.core.Either
import libs.errs.LogicError
import microarch.delivery.application.queries.dto.CourierDto

interface GetAllCouriersQueryHandler {
    fun handle(query: GetAllCouriersQuery): Either<LogicError, List<CourierDto>>
}
