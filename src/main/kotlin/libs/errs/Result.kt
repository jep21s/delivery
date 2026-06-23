package libs.errs

class Result<T, E : Error> private constructor(
    private val value: T?,
    private val error: E?,
    private val success: Boolean,
) {
    fun isSuccess(): Boolean = success

    fun isFailure(): Boolean = !success

    fun getValue(): T {
        check(success) { "Cannot get value from failure" }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    fun getError(): E {
        check(!success) { "Cannot get error from success" }
        return error!!
    }

    fun <U> map(mapper: (T) -> U): Result<U, E> = if (success) success(mapper(getValue())) else failure(error!!)

    fun <U> flatMap(mapper: (T) -> Result<U, E>): Result<U, E> = if (success) mapper(getValue()) else failure(error!!)

    fun onSuccess(handler: (T) -> Unit): Result<T, E> {
        if (success) handler(getValue())
        return this
    }

    fun onFailure(handler: (E) -> Unit): Result<T, E> {
        if (!success) handler(error!!)
        return this
    }

    fun <U> fold(
        onSuccess: (T) -> U,
        onFailure: (E) -> U,
    ): U = if (success) onSuccess(getValue()) else onFailure(error!!)

    fun <F : Error> mapError(mapper: (E) -> F): Result<T, F> = if (success) success(getValue()) else failure(mapper(error!!))

    /**
     * Fail-fast для домена. Использовать ТОЛЬКО там, где ошибка невозможна по контракту.
     */
    fun getValueOrThrow(): T = if (success) getValue() else throw DomainInvariantException(error!!)

    override fun toString(): String = if (success) "Success($value)" else "Failure($error)"

    companion object {
        fun <T, E : Error> success(value: T): Result<T, E> = Result(value, null, true)

        fun <E : Error> success(): Result<Unit, E> = Result(Unit, null, true)

        fun <T, E : Error> failure(error: E): Result<T, E> = Result(null, error, false)
    }
}
