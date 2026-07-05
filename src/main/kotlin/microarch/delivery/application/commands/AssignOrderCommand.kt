package microarch.delivery.application.commands

import arrow.core.Either
import arrow.core.right
import libs.errs.LogicError

class AssignOrderCommand private constructor() {
    companion object {
        fun create(): Either<LogicError, AssignOrderCommand> = AssignOrderCommand().right()
    }
}
