package libs.errs

import java.util.Objects

class Error private constructor(
    val code: String,
    val message: String,
) {
    fun serialize(): String = "$code$SEPARATOR$message"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Error) return false
        return code == other.code && message == other.message
    }

    override fun hashCode(): Int = Objects.hash(code, message)

    override fun toString(): String = "Error{code='$code', message='$message'}"

    companion object {
        private const val SEPARATOR = "||"

        fun of(
            code: String,
            message: String,
        ): Error = Error(code, message)

        fun deserialize(serialized: String): Error {
            if ("A non-empty request body is required." == serialized) {
                return GeneralErrors.valueIsRequired("serialized")
            }

            val parts = serialized.split("\\|\\|".toRegex())

            require(parts.size >= 2) { "Invalid error serialization: '$serialized'" }

            return Error(parts[0], parts[1])
        }

        fun throwIf(error: Error?) {
            if (error != null) throw DomainInvariantException(error)
        }
    }
}
