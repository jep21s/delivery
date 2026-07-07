package microarch.delivery

import libs.errs.DomainInvariantException
import microarch.delivery.adapters.inbound.http.model.Error
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(DomainInvariantException::class)
    fun handleDomainInvariant(ex: DomainInvariantException): ResponseEntity<Error> =
        ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                Error(
                    code = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    message = ex.message ?: "Domain invariant",
                ),
            )

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(): ResponseEntity<Error> =
        ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                Error(
                    code = HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    message = "Internal server error",
                ),
            )
}
