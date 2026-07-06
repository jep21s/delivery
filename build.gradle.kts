import java.net.URI
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.spring") version "2.3.20"
    kotlin("plugin.jpa") version "2.3.20"
    kotlin("plugin.allopen") version "2.3.20"
    id("com.google.devtools.ksp") version "2.3.9"

    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.6"

    id("com.google.protobuf") version "0.9.4"
    id("org.openapi.generator") version "7.10.0"

    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    // TODO(detekt): вернуть после выхода detekt 2.0 stable + апгрейда Gradle до 9.x.
    //  detekt 1.23.x несовместим с Kotlin 2.3.20 (скомпилирован Kotlin 2.0.21),
    //  detekt 2.0-alpha требует Gradle 9.x и не опубликован на Plugin Portal.
    //  До тех пор используется только ktlint.
    // id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "microarch"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

// ---------------------------------------------------------------------------
// Версии зависимостей, не управляемых Spring Boot BOM
// ---------------------------------------------------------------------------
val grpcVersion = "1.57.2"
val protobufVersion = "3.25.5"
val konvertVersion = "4.5.0"
val testcontainersVersion = "1.21.3"
val swaggerAnnotationsVersion = "2.2.14"
val arrowVersion = "2.2.2.1"
val mockkVersion = "1.13.13"
val springmockkVersion = "4.0.2"
val kotlinLoggingVersion = "7.0.3"

dependencies {
    // --- Spring Boot ------------------------------------------------------
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.springframework.kafka:spring-kafka")

    // --- БД ---------------------------------------------------------------
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")

    // --- Kotlin / Jackson -------------------------------------------------
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")

    // --- Логирование (Kotlin-friendly facade поверх SLF4J) ----------------
    implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")

    // --- OpenAPI (аннотации для kotlin-spring генератора) -----------------
    implementation("io.swagger.core.v3:swagger-annotations:$swaggerAnnotationsVersion")

    // --- gRPC / Protobuf (генерируется из src/main/proto) -----------------
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
    compileOnly("jakarta.annotation:jakarta.annotation-api:2.1.1")
    // Требуется сгенерированным gRPC-стабам (GeoGrpc.java использует @javax.annotation.Generated)
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")

    // --- Konvert (KSP-based маппер, замена MapStruct) ---------------------
    implementation("io.mcarle:konvert-annotations:$konvertVersion")
    ksp("io.mcarle:konvert:$konvertVersion")

    // --- Arrow (замена самописного Result) --------------------------------
    implementation("io.arrow-kt:arrow-core-jvm:$arrowVersion")

    // --- Тесты ------------------------------------------------------------
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:kafka:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.reflections:reflections:0.10.2")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.ninja-squad:springmockk:$springmockkVersion")
}

// ---------------------------------------------------------------------------
// Kotlin compiler
// ---------------------------------------------------------------------------
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        // Включает флаг -parameters (имена параметров в reflection) корректным
        // для KSP способом (freeCompilerArgs ломает KSP-задачу).
        javaParameters.set(true)
    }
}

allOpen {
    // Гарантируем, что JPA @MappedSuperclass-классы (BaseEntity, Aggregate)
    // и DomainEvent (extends ApplicationEvent) открыты для наследования и JPA.
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Entity")
    annotation("org.springframework.context.event.EventListener")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ---------------------------------------------------------------------------
// Spring Boot: сохранить родной JVM-аргумент из исходного pom.xml
// ---------------------------------------------------------------------------
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// ---------------------------------------------------------------------------
// OpenAPI codegen → Kotlin (kotlin-spring)
// ---------------------------------------------------------------------------
val openapiOutputDir =
    layout.buildDirectory
        .dir("generated/openapi")
        .get()
        .asFile.path

// Удалённый спецификатор скачиваем в локальный файл — gradle-плагин openapi-generator
// валидирует inputSpec как локальный файл.
val openapiSpecUrl =
    "https://gitlab.com/microarch-ru/ddd-in-practice/system-design/-/raw/main/" +
        "services/delivery/contracts/openapi.yml?ref_type=heads"
val openapiSpecFile = layout.buildDirectory.file("openapi-spec/openapi.yml")

val downloadOpenapiSpec by tasks.registering {
    val url = openapiSpecUrl
    val outFile = openapiSpecFile
    inputs.property("specUrl", url)
    outputs.file(outFile)
    doLast {
        val target = outFile.get().asFile
        target.parentFile.mkdirs()
        val content = URI(url).toURL().readText()
        // kotlin-spring генератор официально поддерживает OpenAPI 3.0.x.
        // Спецификация курса объявлена как 3.1.0, но не использует 3.1-специфичных
        // конструкций — понижаем версию до 3.0.3 (обратная совместимость).
        val patched =
            content.replaceFirst(
                Regex("""(?m)^openapi:\s*['"]?3\.1\.\d+['"]?\s*$"""),
                "openapi: \"3.0.3\"",
            )
        target.writeText(patched)
    }
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set(openapiSpecFile.map { it.asFile.path })
    outputDir.set(openapiOutputDir)
    apiPackage.set("microarch.delivery.adapters.inbound.http.api")
    modelPackage.set("microarch.delivery.adapters.inbound.http.model")

    generateApiTests.set(false)
    generateModelTests.set(false)

    configOptions.set(
        mapOf(
            "useSpringBoot3" to "true",
            "interfaceOnly" to "true",
        ),
    )

    globalProperties.set(
        mapOf(
            "modelDocs" to "false",
            "apiDocs" to "false",
        ),
    )
}

tasks.named("openApiGenerate") { dependsOn(downloadOpenapiSpec) }

// Подключаем сгенерированный Kotlin-код в основной source set
kotlin.sourceSets
    .getByName("main")
    .kotlin
    .srcDir("$openapiOutputDir/src/main/kotlin/microarch")

// ---------------------------------------------------------------------------
// Protobuf + gRPC codegen → Java (вызывается из Kotlin через interop)
// ---------------------------------------------------------------------------
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

// Порядок: Kotlin-компиляция и KSP ждут генерацию OpenAPI.
// kspKotlin создаётся KSP-плагином лениво, поэтому используем matching.
tasks.matching { it.name == "compileKotlin" || it.name == "kspKotlin" }.configureEach {
    dependsOn("openApiGenerate")
}
// compileJava автоматически зависит от generateProto через protobuf-плагин.

apply(from = "gradle/konvert-postprocess.gradle.kts")

// ---------------------------------------------------------------------------
// ktlint + detekt
// ---------------------------------------------------------------------------
ktlint {
    version.set("1.8.0")
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    filter {
        exclude { it.file.path.contains("/generated/") || it.file.path.contains("/build/") }
    }
}

// TODO(detekt): восстановить блок конфигурации вместе с плагином после выхода detekt 2.0 stable.
// detekt {
//     buildUponDefaultConfig = true
//     allRules = false
//     config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
//     ignoreFailures = false
//     source.setFrom(files("src/main/kotlin", "src/test/kotlin"))
// }
//
// tasks.withType<Detekt>().configureEach {
//     exclude("$openapiOutputDir/**")
//     exclude("**/generated/**")
//     jvmTarget = "21"
// }
