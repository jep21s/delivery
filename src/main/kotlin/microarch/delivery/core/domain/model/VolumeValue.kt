package microarch.delivery.core.domain.model

import libs.ddd.ValueObject

class VolumeValue(
    val value: Int,
) : ValueObject<VolumeValue>() {
    private val components: List<Int> by lazy(LazyThreadSafetyMode.NONE) { listOf(value) }

    override fun equalityComponents(): Iterable<Any> = components

    operator fun plus(other: VolumeValue) = VolumeValue(this.value + other.value)
}

fun List<VolumeValue>.sum(): VolumeValue {
    var result = 0
    this.forEach { result += it.value }
    return VolumeValue(result)
}
