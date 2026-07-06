import java.io.File

// Пост-процессинг сгенерированных Konvert-мапперов: заменяем `!!` (генерируется
// при `konvert.enforce-not-null=true`) на `requireNotNull(fieldName = ...)` из
// `libs.konvert`, чтобы получить осмысленное сообщение вместо голого NPE.
//
// Применяется как `apply(from = ...)` из основного build.gradle.kts, поэтому не
// импортирует типы Kotlin-плагина (их classpath недоступен во внешнем скрипте).
// Навешиваемся на KSP-задачу: Konvert генерирует `*Impl.kt` в `build/generated/ksp/`
// во время kspKotlin — подменяем `!!` сразу после генерации, до compileKotlin.

afterEvaluate {
    tasks.matching { it.name == "kspKotlin" }.configureEach {
        doLast { processKonvertFiles() }
    }
}

fun processKonvertFiles() {
    val generatedDir = file("build/generated/ksp")
    if (!generatedDir.exists()) return

    val customImport = "import libs.konvert.requireNotNull"

    generatedDir.walk()
        .filter { it.isFile && it.extension == "kt" }
        .forEach { file ->
            var content = file.readText()

            // Пропускаем файлы без !!
            if (!content.contains("!!")) return@forEach

            // Добавляем импорт если нужно
            if (!content.contains(customImport)) {
                content = addImportSafely(content, customImport)
            }

            // Заменяем expr!! на expr.requireNotNull(fieldName = "expr")
            val pattern = """([\w\[\]().]+?(?:\.\w+)*)!!""".toRegex()
            content = pattern.replace(content) { match ->
                val expr = match.groupValues[1]
                "$expr.requireNotNull(fieldName = \"$expr\")"
            }

            // Заменяем окончания функций }!! на }.requireNotNull()
            val patternFunction = """}!!""".toRegex()
            content = patternFunction.replace(content) { "}.requireNotNull()" }

            file.writeText(content)
        }
}

fun addImportSafely(content: String, importToAdd: String): String {
    val lines = content.lines().toMutableList()

    val importLines = lines.filter { it.startsWith("import ") }
    val existingSimilarImport = importLines.find { it == importToAdd }
    if (existingSimilarImport != null) {
        lines.remove(existingSimilarImport)
    }

    val lastImportIndex = lines.indexOfLast { it.startsWith("import ") }
    if (lastImportIndex != -1) {
        lines.add(lastImportIndex + 1, importToAdd)
    } else {
        val packageIndex = lines.indexOfFirst { it.startsWith("package ") }
        if (packageIndex != -1) {
            lines.add(packageIndex + 1, importToAdd)
        } else {
            lines.add(0, importToAdd)
        }
    }

    return lines.joinToString("\n")
}
