package microarch.delivery.application.queries

import arrow.core.Either
import arrow.core.right
import libs.errs.LogicError

class GetAllCouriersQuery private constructor() {
    companion object {
        fun create(): Either<LogicError, GetAllCouriersQuery> = GetAllCouriersQuery().right()
    }
}
