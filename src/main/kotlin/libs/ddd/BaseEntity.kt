package libs.ddd

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.util.Objects

@MappedSuperclass
abstract class BaseEntity<TId : Comparable<TId>> protected constructor(
    id: TId? = null,
) : Comparable<BaseEntity<TId>> {
    @field:Id
    @field:Column(name = "id")
    open var id: TId? = id
        protected set

    protected fun isTransient(): Boolean = id == null || id == defaultValue()

    protected open fun defaultValue(): TId? = null

    override fun equals(obj: Any?): Boolean {
        if (obj == null) return false
        if (this === obj) return true
        if (obj !is BaseEntity<*>) return false
        if (this.javaClass != obj.javaClass) return false
        if (this.isTransient() || obj.isTransient()) return false
        return Objects.equals(this.id, obj.id)
    }

    override fun hashCode(): Int = (javaClass.name + (id?.toString() ?: "")).hashCode()

    override fun compareTo(other: BaseEntity<TId>): Int {
        if (this === other) return 0
        return this.id!!.compareTo(other.id!!)
    }
}
