package microarch.delivery.application.commands

import arrow.core.Either
import libs.errs.LogicError

interface MoveCourierCommandHandler {
    fun handle(command: MoveCourierCommand): Either<LogicError, Unit>
}
