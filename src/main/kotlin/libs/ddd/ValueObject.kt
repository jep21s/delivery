package libs.ddd

import java.math.BigDecimal
import java.util.Objects

abstract class ValueObject<T : ValueObject<T>> : Comparable<T> {
    protected abstract fun equalityComponents(): Iterable<Any?>

    private fun toList(iterable: Iterable<Any?>): List<Any?> = iterable.toList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ValueObject<*>

        val thisComponents: List<Any?> = toList(this.equalityComponents())
        val thatComponents: List<Any?> = toList(that.equalityComponents())

        if (thisComponents.size != thatComponents.size) return false

        for (i in thisComponents.indices) {
            if (!Objects.equals(thisComponents[i], thatComponents[i])) return false
        }
        return true
    }

    override fun hashCode(): Int = Objects.hash(*toList(equalityComponents()).toTypedArray())

    override fun compareTo(other: T): Int {
        val thisComponents: List<Any?> = toList(this.equalityComponents())
        val otherComponents: List<Any?> = toList(other.equalityComponents())

        var i = 0
        while (i < thisComponents.size && i < otherComponents.size) {
            val result = safeCompare(thisComponents[i], otherComponents[i])
            if (result != 0) return result
            i++
        }
        return thisComponents.size.compareTo(otherComponents.size)
    }

    override fun toString(): String {
        val components = toList(equalityComponents())
        val sb = StringBuilder(javaClass.simpleName).append("[")
        for (i in components.indices) {
            sb.append(components[i])
            if (i < components.size - 1) sb.append(", ")
        }
        sb.append("]")
        return sb.toString()
    }

    companion object {
        @JvmStatic
        protected fun safeCompare(
            a: Any?,
            b: Any?,
        ): Int {
            if (a === b) return 0
            if (a == null) return -1
            if (b == null) return 1

            if (a is BigDecimal && b is BigDecimal) {
                return a.compareTo(b)
            }

            require(a is Comparable<*> && b is Comparable<*>) { "Fields must be Comparable" }

            @Suppress("UNCHECKED_CAST")
            return (a as Comparable<Any?>).compareTo(b)
        }
    }
}
