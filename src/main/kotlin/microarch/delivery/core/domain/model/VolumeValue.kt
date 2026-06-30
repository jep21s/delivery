package microarch.delivery.core.domain.model

import jakarta.persistence.Embeddable
import jakarta.persistence.Transient
import libs.ddd.ValueObject

@Embeddable
class VolumeValue(
    val value: Int,
) : ValueObject<VolumeValue>() {
    @field:Transient
    private val components: List<Int> = listOf(value)

    override fun equalityComponents(): Iterable<Any> = components

    operator fun plus(other: VolumeValue) = VolumeValue(this.value + other.value)
}

fun List<VolumeValue>.sum(): VolumeValue {
    var result = 0
    this.forEach { result += it.value }
    return VolumeValue(result)
}
