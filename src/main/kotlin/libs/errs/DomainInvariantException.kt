package libs.errs

class DomainInvariantException(
    error: LogicError,
) : RuntimeException("Domain invariant violated: ${error.fullMessage()}")
