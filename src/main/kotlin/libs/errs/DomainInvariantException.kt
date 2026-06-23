package libs.errs

class DomainInvariantException(
    error: Error,
) : RuntimeException("Domain invariant violated: ${error.message}")
