package microarch.delivery.application.commands

import arrow.core.Either
import libs.errs.LogicError

interface AssignOrderCommandHandler {
    fun handle(command: AssignOrderCommand): Either<LogicError, Unit>
}
