package libs.ddd

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.util.Objects

@MappedSuperclass
abstract class BaseEntity<TId : Comparable<TId>> protected constructor(
    @field:Id
    @field:Column(name = "id")
    val id: TId,
) : Comparable<BaseEntity<TId>> {
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (this === other) return true
        if (other !is BaseEntity<*>) return false
        if (this.javaClass != other.javaClass) return false
        return Objects.equals(this.id, other.id)
    }

    override fun hashCode(): Int = (javaClass.name + (id.toString())).hashCode()

    override fun compareTo(other: BaseEntity<TId>): Int {
        if (this === other) return 0
        return this.id.compareTo(other.id)
    }
}
