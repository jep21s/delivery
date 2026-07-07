package microarch.delivery.application.commands

import arrow.core.Either
import libs.errs.LogicError

interface CreateCourierCommandHandler {
    fun handle(command: CreateCourierCommand): Either<LogicError, Unit>
}
