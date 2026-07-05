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
    override fun equals(obj: Any?): Boolean {
        if (obj == null) return false
        if (this === obj) return true
        if (obj !is BaseEntity<*>) return false
        if (this.javaClass != obj.javaClass) return false
        return Objects.equals(this.id, obj.id)
    }

    override fun hashCode(): Int = (javaClass.name + (id.toString())).hashCode()

    override fun compareTo(other: BaseEntity<TId>): Int {
        if (this === other) return 0
        return this.id.compareTo(other.id)
    }
}
