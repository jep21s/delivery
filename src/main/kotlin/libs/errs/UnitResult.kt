package libs.errs

class UnitResult<E : Error> private constructor(
    private val success: Boolean,
    private val error: E?,
) {
    fun isSuccess(): Boolean = success

    fun isFailure(): Boolean = !success

    fun getError(): E {
        check(!success) { "Cannot get error from success" }
        return error!!
    }

    fun onSuccess(handler: () -> Unit): UnitResult<E> {
        if (success) handler()
        return this
    }

    fun onFailure(handler: (E) -> Unit): UnitResult<E> {
        if (!success) handler(error!!)
        return this
    }

    fun <U> fold(
        onSuccess: () -> U,
        onFailure: (E) -> U,
    ): U = if (success) onSuccess() else onFailure(error!!)

    fun merge(other: UnitResult<E>): UnitResult<E> =
        when {
            !success -> this
            !other.success -> other
            else -> success()
        }

    fun toResult(): Result<Unit, E> = if (!success) Result.failure(error!!) else Result.success()

    fun getOrElseThrow(exceptionMapper: (E) -> RuntimeException) {
        if (success) return
        throw exceptionMapper(error!!)
    }

    /**
     * Fail-fast для домена. Использовать ТОЛЬКО там, где ошибка невозможна по контракту.
     */
    fun getOrElseThrow() {
        if (success) return
        throw DomainInvariantException(error!!)
    }

    override fun toString(): String = if (success) "Success" else "Failure($error)"

    companion object {
        fun <E : Error> success(): UnitResult<E> = UnitResult(true, null)

        fun <E : Error> failure(error: E): UnitResult<E> = UnitResult(false, error)

        fun <E : Error> from(result: Result<Unit, E>): UnitResult<E> = if (result.isSuccess()) success() else failure(result.getError())
    }
}
