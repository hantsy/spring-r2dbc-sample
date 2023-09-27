import nu.studer.gradle.jooq.JooqEdition
import nu.studer.gradle.jooq.JooqGenerate
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.meta.jaxb.Property

plugins {
    id("org.springframework.boot") version "3.1.4"
    id("io.spring.dependency-management") version "1.1.3"
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.spring") version "1.9.10"
    id("nu.studer.jooq") version "8.2.1"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

extra["testcontainersVersion"] = "1.19.0"
val kotlinCoVersion = project.properties["kotlinCoVersion"]
val kotestVersion = project.properties["kotestVersion"]
val mockkVersion = project.properties["mockkVersion"]
val springmockkVersion = project.properties["springmockkVersion"]
val jooqVersion = project.properties["jooqVersion"]

dependencies {

    // webflux
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    //kotlin and coroutines support
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    //r2dbc and spring data r2dbc
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    //jooq
    implementation("org.jooq:jooq:${jooqVersion}")
    implementation("org.jooq:jooq-kotlin:${jooqVersion}")
    // workaround of issue: https://github.com/etiennestuder/gradle-jooq-plugin/issues/209
    jooqGenerator("jakarta.xml.bind:jakarta.xml.bind-api:4.0.1")
    jooqGenerator("org.jooq:jooq-meta-extensions:${jooqVersion}")
    // workaround of array type codegen, see: https://github.com/jOOQ/jOOQ/issues/13322
    jooqGenerator("com.h2database:h2:2.2.224")

    // test dependencies
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        // use mockk as mocking framework
        exclude(module = "mockito-core")
    }
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:r2dbc")

    // test helpers for Kotlin coroutines
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${kotlinCoVersion}")

    // Kotest assertions
    testImplementation("io.kotest:kotest-assertions-core-jvm:${kotestVersion}")
    testImplementation("io.kotest:kotest-framework-concurrency-jvm:${kotestVersion}")

    // mockk: mocking framework for Kotlin
    testImplementation("io.mockk:mockk-jvm:${mockkVersion}")

    // mockk spring integration
    testImplementation("com.ninja-squad:springmockk:${springmockkVersion}")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jooq {
    version.set("$jooqVersion")  // the default (can be omitted)
    edition.set(JooqEdition.OSS)  // the default (can be omitted)

    configurations {
        create("main") {  // name of the jOOQ configuration
            generateSchemaSourceOnCompilation.set(true)  // default (can be omitted)

            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc = null; // only required for gen from active databases.

                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.extensions.ddl.DDLDatabase" // gen from ddl schema.

                        // commoutted out this, see: https://github.com/etiennestuder/gradle-jooq-plugin/issues/222
                        // inputSchema = "public"
                        properties.addAll(
                            listOf(
                                // Specify the location of your SQL script.
                                // You may use ant-style file matching, e.g. /path/**/to/*.sql
                                //
                                // Where:
                                // - ** matches any directory subtree
                                // - * matches any number of characters in a directory / file name
                                // - ? matches a single character in a directory / file name
                                Property().apply {
                                    key = "scripts"
                                    value = "src/main/resources/schema.sql"
                                },

                                // The sort order of the scripts within a directory, where:
                                //
                                // - semantic: sorts versions, e.g. v-3.10.0 is after v-3.9.0 (default)
                                // - alphanumeric: sorts strings, e.g. v-3.10.0 is before v-3.9.0
                                // - flyway: sorts files the same way as flyway does
                                // - none: doesn't sort directory contents after fetching them from the directory
                                Property().apply {
                                    key = "sort"
                                    value = "semantic"
                                },

                                // The default schema for unqualified objects:
                                //
                                // - public: all unqualified objects are located in the PUBLIC (upper case) schema
                                // - none: all unqualified objects are located in the default schema (default)
                                //
                                // This configuration can be overridden with the schema mapping feature
                                Property().apply {
                                    key = "unqualifiedSchema"
                                    value = "none"
                                },

                                // The default name case for unquoted objects:
                                //
                                // - as_is: unquoted object names are kept unquoted
                                // - upper: unquoted object names are turned into upper case (most databases)
                                // - lower: unquoted object names are turned into lower case (e.g. PostgreSQL)
                                Property().apply {
                                    key = "defaultNameCase"
                                    value = "lower"
                                }
                            )
                        )
                    }
                    generate.apply {
                        isPojosAsKotlinDataClasses = true // use data classes
                    }
                    target.apply {
                        packageName = "com.example.demo.jooq"
                        directory = "build/generated-src/jooq/main"  // default (can be omitted)
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

// participate in incremental builds and build caching
tasks.named<JooqGenerate>("generateJooq") {
    allInputsDeclared.set(true)
}
