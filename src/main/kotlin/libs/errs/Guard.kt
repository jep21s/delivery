package libs.errs

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.util.UUID

object Guard {
    private val EMPTY_UUID = UUID(0L, 0L)

    fun againstNullOrEmpty(
        value: String?,
        paramName: String,
    ): Either<LogicError, Unit> = if (value.isNullOrBlank()) GeneralErrors.valueIsRequired(paramName).left() else Unit.right()

    fun againstNullOrEmpty(
        collection: Collection<*>?,
        paramName: String,
    ): Either<LogicError, Unit> = if (collection.isNullOrEmpty()) GeneralErrors.valueIsRequired(paramName).left() else Unit.right()

    fun againstNullOrEmpty(
        uuid: UUID?,
        paramName: String,
    ): Either<LogicError, Unit> = if (uuid == null || uuid == EMPTY_UUID) GeneralErrors.valueIsRequired(paramName).left() else Unit.right()

    fun <T : Comparable<T>> againstGreaterThan(
        value: T,
        max: T,
        paramName: String,
    ): Either<LogicError, Unit> = if (value > max) GeneralErrors.valueMustBeLessThan(paramName, value, max).left() else Unit.right()

    fun <T : Comparable<T>> againstGreaterOrEqual(
        value: T,
        max: T,
        paramName: String,
    ): Either<LogicError, Unit> = if (value >= max) GeneralErrors.valueMustBeLessOrEqual(paramName, value, max).left() else Unit.right()

    fun <T : Comparable<T>> againstLessThan(
        value: T,
        min: T,
        paramName: String,
    ): Either<LogicError, Unit> = if (value < min) GeneralErrors.valueMustBeGreaterThan(paramName, value, min).left() else Unit.right()

    fun <T : Comparable<T>> againstLessOrEqual(
        value: T,
        min: T,
        paramName: String,
    ): Either<LogicError, Unit> = if (value <= min) GeneralErrors.valueMustBeGreaterOrEqual(paramName, value, min).left() else Unit.right()

    fun <T : Comparable<T>> againstOutOfRange(
        value: T,
        min: T,
        max: T,
        paramName: String,
    ): Either<LogicError, Unit> =
        if (value < min || value > max) {
            GeneralErrors.valueIsOutOfRange(paramName, value, min, max).left()
        } else {
            Unit.right()
        }
}
