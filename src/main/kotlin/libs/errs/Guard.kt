package libs.errs

import java.util.UUID

object Guard {
    private val EMPTY_UUID = UUID(0L, 0L)

    fun combine(vararg errors: Error?): Error? = errors.firstOrNull { it != null }

    fun againstNullOrEmpty(
        value: String?,
        paramName: String,
    ): Error? = if (value.isNullOrBlank()) GeneralErrors.valueIsRequired(paramName) else null

    fun againstNullOrEmpty(
        collection: Collection<*>?,
        paramName: String,
    ): Error? = if (collection.isNullOrEmpty()) GeneralErrors.valueIsRequired(paramName) else null

    fun againstNullOrEmpty(
        uuid: UUID?,
        paramName: String,
    ): Error? = if (uuid == null || uuid == EMPTY_UUID) GeneralErrors.valueIsRequired(paramName) else null

    fun <T : Comparable<T>> againstGreaterThan(
        value: T,
        max: T,
        paramName: String,
    ): Error? = if (value > max) GeneralErrors.valueMustBeLessThan(paramName, value, max) else null

    fun <T : Comparable<T>> againstGreaterOrEqual(
        value: T,
        max: T,
        paramName: String,
    ): Error? = if (value >= max) GeneralErrors.valueMustBeLessOrEqual(paramName, value, max) else null

    fun <T : Comparable<T>> againstLessThan(
        value: T,
        min: T,
        paramName: String,
    ): Error? = if (value < min) GeneralErrors.valueMustBeGreaterThan(paramName, value, min) else null

    fun <T : Comparable<T>> againstLessOrEqual(
        value: T,
        min: T,
        paramName: String,
    ): Error? = if (value <= min) GeneralErrors.valueMustBeGreaterOrEqual(paramName, value, min) else null

    fun <T : Comparable<T>> againstOutOfRange(
        value: T,
        min: T,
        max: T,
        paramName: String,
    ): Error? = if (value < min || value > max) GeneralErrors.valueIsOutOfRange(paramName, value, min, max) else null
}
