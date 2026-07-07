package microarch.delivery.application.commands

import arrow.core.Either
import java.util.UUID
import libs.errs.LogicError

interface CreateCourierCommandHandler {
    fun handle(command: CreateCourierCommand): Either<LogicError, UUID>
}
