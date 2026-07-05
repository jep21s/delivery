package microarch.delivery.application.commands

import arrow.core.Either
import arrow.core.raise.either
import java.util.UUID
import libs.errs.Guard
import libs.errs.LogicError
import microarch.delivery.core.domain.model.LocationValue

class MoveCourierCommand private constructor(
    val courierId: UUID,
    val location: LocationValue,
) {
    companion object {
        fun create(
            courierId: UUID,
            x: Int,
            y: Int,
        ): Either<LogicError, MoveCourierCommand> =
            either {
                Guard.againstNullOrEmpty(courierId, "courierId").bind()
                val location = LocationValue.create(x, y).bind()
                MoveCourierCommand(courierId, location)
            }
    }
}
