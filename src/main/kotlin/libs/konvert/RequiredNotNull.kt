package libs.konvert

@Suppress("unused")
fun <T> T?.requireNotNull(fieldName: String): T = requireNotNull(this) { "Field '$fieldName' is required" }

@Suppress("unused")
fun <T> T?.requireNotNull(): T = requireNotNull(this)
