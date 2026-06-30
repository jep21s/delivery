package microarch.delivery.core.domain.model

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.accumulate
import arrow.core.raise.either
import libs.ddd.ValueObject
import libs.errs.Error
import libs.errs.Guard
import libs.errs.getValueOrThrow

class LocationValue private constructor(
    val x: Int,
    val y: Int,
) : ValueObject<LocationValue>() {
    private val components: List<Int> by lazy(LazyThreadSafetyMode.NONE) { listOf(x, y) }

    fun distanceTo(other: LocationValue): Int {
        val xResult: Int = subtract(this.x, other.x)
        val yResult: Int = subtract(this.y, other.y)
        return xResult + yResult
    }

    private fun subtract(
        thisCoordinate: Int,
        otherCoordinate: Int,
    ): Int =
        if (thisCoordinate >= otherCoordinate) {
            thisCoordinate - otherCoordinate
        } else {
            otherCoordinate - thisCoordinate
        }

    override fun equalityComponents(): Iterable<Any> = components

    companion object {
        private const val MIN_COORDINATE_VALUE: Int = 1
        private const val MAX_COORDINATE_VALUE: Int = 10

        fun create(
            x: Int,
            y: Int,
        ): Either<Error, LocationValue> =
            either<NonEmptyList<Error>, LocationValue> {
                accumulate {
                    Guard.againstLessThan(x, MIN_COORDINATE_VALUE, LocationValue::x.name).bind()
                    Guard.againstLessThan(y, MIN_COORDINATE_VALUE, LocationValue::y.name).bind()
                    Guard.againstGreaterThan(x, MAX_COORDINATE_VALUE, LocationValue::x.name).bind()
                    Guard.againstGreaterThan(y, MAX_COORDINATE_VALUE, LocationValue::y.name).bind()
                    LocationValue(x, y)
                }
            }.mapLeft { nel -> Error.of(nel.toList()) }

        fun createOrThrow(
            x: Int,
            y: Int,
        ): LocationValue = create(x, y).getValueOrThrow()
    }
}
