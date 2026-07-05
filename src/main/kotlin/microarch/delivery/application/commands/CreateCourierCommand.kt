package microarch.delivery.application.commands

import arrow.core.Either
import arrow.core.raise.either
import libs.errs.Guard
import libs.errs.LogicError

class CreateCourierCommand private constructor(
    val name: String,
) {
    companion object {
        fun create(name: String): Either<LogicError, CreateCourierCommand> =
            either {
                Guard.againstNullOrEmpty(name, "name").bind()
                CreateCourierCommand(name)
            }
    }
}
