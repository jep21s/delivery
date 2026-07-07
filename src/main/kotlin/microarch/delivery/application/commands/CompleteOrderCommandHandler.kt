package microarch.delivery.application.commands

import arrow.core.Either
import libs.errs.LogicError

interface CompleteOrderCommandHandler {
    fun handle(command: CompleteOrderCommand): Either<LogicError, Unit>
}
