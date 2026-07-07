package microarch.delivery.adapters.`in`.http

import libs.errs.LogicError
import microarch.delivery.adapters.inbound.http.model.Error
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

internal fun LogicError.toHttpStatus(): HttpStatus =
    when {
        code == "404" || code == "record.not.found" -> HttpStatus.NOT_FOUND
        isValidationError() -> HttpStatus.BAD_REQUEST
        else -> HttpStatus.CONFLICT
    }

private fun LogicError.isValidationError(): Boolean =
    code.startsWith("value.") ||
        code.startsWith("collection.") ||
        code == "invalid.string.length"

internal fun LogicError.toApiError(): Error =
    Error(
        code = toHttpStatus().value(),
        message = fullMessage(),
    )

/**
 * Унифицированный построитель ответа с ошибкой для использования внутри `getOrElse`/`fold`.
 * Возвращает параметризованный [ResponseEntity] через непроверяемый cast, чтобы Kotlin
 * выводил общий тип возврата у веток `Either.fold`.
 */
@Suppress("UNCHECKED_CAST")
internal fun <T> LogicError.toErrorResponse(): ResponseEntity<T> =
    ResponseEntity
        .status(toHttpStatus())
        .body(toApiError()) as ResponseEntity<T>
