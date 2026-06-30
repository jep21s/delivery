package libs.errs

import arrow.core.Either
import arrow.core.getOrElse

/**
 * Fail-fast: возвращает значение из [Either] либо бросает [DomainInvariantException].
 * Использовать только там, где ошибка невозможна по контракту.
 */
fun <T> Either<Error, T>.getValueOrThrow(): T = getOrElse { throw DomainInvariantException(it) }

/**
 * Fail-fast: возвращает значение из [Either] либо бросает исключение,
 * построенное [exceptionMapper] из доменной ошибки.
 */
fun <T> Either<Error, T>.getOrElseThrow(exceptionMapper: (Error) -> RuntimeException): T = getOrElse { throw exceptionMapper(it) }

/**
 * Бросает [DomainInvariantException], если ошибка не null.
 */
fun Error?.throwIf() {
    if (this != null) throw DomainInvariantException(this)
}
