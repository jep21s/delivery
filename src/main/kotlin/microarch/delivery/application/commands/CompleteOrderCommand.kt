package microarch.delivery.application.commands

import arrow.core.Either
import arrow.core.raise.either
import java.util.UUID
import libs.errs.Guard
import libs.errs.LogicError

class CompleteOrderCommand private constructor(
    val courierId: UUID,
    val orderId: UUID,
) {
    companion object {
        fun create(
            courierId: UUID,
            orderId: UUID,
        ): Either<LogicError, CompleteOrderCommand> =
            either {
                Guard.againstNullOrEmpty(courierId, "courierId").bind()
                Guard.againstNullOrEmpty(orderId, "orderId").bind()
                CompleteOrderCommand(courierId, orderId)
            }
    }
}
