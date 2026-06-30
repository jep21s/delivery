package libs.errs

object GeneralErrors {
    fun <T> notFound(
        name: String,
        id: T,
    ): LogicError {
        require(name.isNotBlank()) { "Name must not be null or empty" }
        return LogicError.of("record.not.found", "Record not found. Name: $name, id: $id")
    }

    fun <T> valueIsInvalid(
        name: String,
        value: T,
    ): LogicError {
        require(name.isNotBlank()) { "Name must not be null or empty" }
        return LogicError.of("value.is.invalid", "Value '$value' is invalid for $name")
    }

    fun valueIsRequired(name: String): LogicError {
        require(name.isNotBlank()) { "Name must not be null or empty" }
        return LogicError.of("value.is.required", "Value is required for $name")
    }

    fun invalidLength(name: String): LogicError {
        require(name.isNotBlank()) { "Name must not be null or empty" }
        return LogicError.of("invalid.string.length", "Invalid $name length")
    }

    fun collectionIsTooSmall(
        min: Int,
        current: Int,
    ): LogicError =
        LogicError.of(
            "collection.is.too.small",
            "The collection must contain $min items or more. It contains $current items.",
        )

    fun collectionIsTooLarge(
        max: Int,
        current: Int,
    ): LogicError =
        LogicError.of(
            "collection.is.too.large",
            "The collection must contain $max items or fewer. It contains $current items.",
        )

    fun <T : Comparable<T>> valueIsOutOfRange(
        name: String,
        value: T,
        min: T,
        max: T,
    ): LogicError {
        require(name.isNotBlank()) { "Name must not be null or empty" }
        val message = "Value $value for $name is out of range. Min value is $min, max value is $max."
        return LogicError.of("value.is.out.of.range", message)
    }

    fun <T : Comparable<T>> valueMustBeGreaterThan(
        name: String,
        value: T,
        min: T,
    ): LogicError {
        require(name.isNotBlank()) { "Name must not be null or empty" }
        return LogicError.of(
            "value.must.be.greater.than",
            "The value of $name ($value) must be greater than $min.",
        )
    }

    fun <T : Comparable<T>> valueMustBeGreaterOrEqual(
        name: String,
        value: T,
        min: T,
    ): LogicError {
        require(name.isNotBlank()) { "Name must not be null or empty" }
        return LogicError.of(
            "value.must.be.greater.or.equal",
            "The value of $name ($value) must be greater than or equal to $min.",
        )
    }

    fun <T : Comparable<T>> valueMustBeLessThan(
        name: String,
        value: T,
        max: T,
    ): LogicError {
        require(name.isNotBlank()) { "Name must not be null or empty" }
        return LogicError.of(
            "value.must.be.less.than",
            "The value of $name ($value) must be less than $max.",
        )
    }

    fun <T : Comparable<T>> valueMustBeLessOrEqual(
        name: String,
        value: T,
        max: T,
    ): LogicError {
        require(name.isNotBlank()) { "Name must not be null or empty" }
        return LogicError.of(
            "value.must.be.less.or.equal",
            "The value of $name ($value) must be less than or equal to $max.",
        )
    }
}
