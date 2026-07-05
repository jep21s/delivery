package microarch.delivery.application.commands

import arrow.core.Either
import libs.errs.LogicError

interface CreateOrderCommandHandler {
    fun handle(command: CreateOrderCommand): Either<LogicError, Unit>
}
