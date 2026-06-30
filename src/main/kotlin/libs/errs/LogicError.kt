package libs.errs

import java.util.Objects

class LogicError private constructor(
    val code: String,
    val message: String,
    val children: List<LogicError> = emptyList(),
) {
    fun fullMessage(): String {
        val messages = mutableListOf<String>()
        collectMessages(messages)
        return messages.joinToString("; ")
    }

    private fun collectMessages(acc: MutableList<String>) {
        if (message.isNotBlank()) acc.add(message)
        children.forEach { it.collectMessages(acc) }
    }

    fun serialize(): String {
        val base = "$code$SEPARATOR$message"
        return if (children.isEmpty()) {
            base
        } else {
            base + CHILDREN_START + children.joinToString(CHILDREN_SEPARATOR) { it.serialize() } + CHILDREN_END
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LogicError) return false
        return code == other.code && message == other.message && children == other.children
    }

    override fun hashCode(): Int = Objects.hash(code, message, children)

    override fun toString(): String =
        if (children.isEmpty()) {
            "Error{code='$code', message='$message'}"
        } else {
            "Error{code='$code', message='$message', children=${children.size}}"
        }

    companion object {
        private const val SEPARATOR = "||"
        private const val CHILDREN_START = "{"
        private const val CHILDREN_END = "}"
        private const val CHILDREN_SEPARATOR = ";"

        fun of(
            code: String,
            message: String,
        ): LogicError = LogicError(code, message)

        fun of(errors: List<LogicError>): LogicError = LogicError("", "", errors)

        fun deserialize(serialized: String): LogicError {
            if ("A non-empty request body is required." == serialized) {
                return GeneralErrors.valueIsRequired("serialized")
            }

            val braceIdx = serialized.indexOf(CHILDREN_START)

            if (braceIdx == -1) {
                val parts = serialized.split(SEPARATOR)
                require(parts.size >= 2) { "Invalid error serialization: '$serialized'" }
                return LogicError(parts[0], parts[1])
            }

            val base = serialized.substring(0, braceIdx)
            val inner = serialized.substring(braceIdx + 1, serialized.length - CHILDREN_END.length)
            val parts = base.split(SEPARATOR)
            require(parts.size >= 2) { "Invalid error serialization: '$serialized'" }

            val childStrings = splitTopLevel(inner)
            val children = childStrings.map { deserialize(it) }

            return LogicError(parts[0], parts[1], children)
        }

        private fun splitTopLevel(str: String): List<String> {
            val result = mutableListOf<String>()
            var depth = 0
            var start = 0
            for (i in str.indices) {
                when (str[i]) {
                    '{' -> {
                        depth++
                    }

                    '}' -> {
                        depth--
                    }

                    CHILDREN_SEPARATOR.first() -> {
                        if (depth == 0) {
                            result.add(str.substring(start, i))
                            start = i + 1
                        }
                    }
                }
            }
            if (start <= str.length && str.isNotEmpty()) {
                result.add(str.substring(start))
            }
            return result
        }
    }
}
