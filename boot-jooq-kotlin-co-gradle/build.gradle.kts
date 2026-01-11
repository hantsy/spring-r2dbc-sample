import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.jooq.jooq-codegen-gradle") version "3.20.10"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

val kotlinCoVersion = project.properties["kotlinCoVersion"]
val kotestVersion = project.properties["kotestVersion"]
val mockkVersion = project.properties["mockkVersion"]
val springmockkVersion = project.properties["springmockkVersion"]
val jooqVersion = project.properties["jooqVersion"]

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("tools.jackson.module:jackson-module-kotlin")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    //jooq
    implementation("org.jooq:jooq:${jooqVersion}")
    implementation("org.jooq:jooq-kotlin:${jooqVersion}")
    // workaround of issue: https://github.com/etiennestuder/gradle-jooq-plugin/issues/209
    jooqCodegen("jakarta.xml.bind:jakarta.xml.bind-api:4.0.4")
    jooqCodegen("org.jooq:jooq-meta-extensions:${jooqVersion}")
    jooqCodegen("org.jooq:jooq-meta-kotlin:${jooqVersion}")
    // workaround of array type codegen, see: https://github.com/jOOQ/jOOQ/issues/13322
    jooqCodegen("com.h2database:h2:2.4.240")

    // test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-data-r2dbc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-r2dbc")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // test helpers for Kotlin coroutines
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${kotlinCoVersion}")

    // Kotest assertions
    testImplementation("io.kotest:kotest-assertions-core-jvm:${kotestVersion}")

    // mockk: mocking framework for Kotlin
    testImplementation("io.mockk:mockk-jvm:${mockkVersion}")

    // mockk spring integration
    testImplementation("com.ninja-squad:springmockk:${springmockkVersion}")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll(
            listOf(
                "-Xjsr305=strict",
                "-Xannotation-default-target=param-property",
                "-opt-in=kotlin.RequiresOptIn"
            )
        )
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("jooqCodegenMain")
}


jooq {
    version = "$jooqVersion"  // the default (can be omitted)
    configuration { }

    executions {
        create("main") {  // name of the jOOQ configuration
            //generateSchemaSourceOnCompilation =true   // default (can be omitted)

            configuration {
                logging = org.jooq.meta.jaxb.Logging.DEBUG
                jdbc = null // only required for gen from active databases.

                generator {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database {
                        name = "org.jooq.meta.extensions.ddl.DDLDatabase" // gen from ddl schema.

                        // commoutted out this, see: https://github.com/etiennestuder/gradle-jooq-plugin/issues/222
                        // inputSchema = "public"
                        properties {

                            // Specify the location of your SQL script.
                            // You may use ant-style file matching, e.g. /path/**/to/*.sql
                            //
                            // Where:
                            // - ** matches any directory subtree
                            // - * matches any number of characters in a directory / file name
                            // - ? matches a single character in a directory / file name
                            property {
                                key = "scripts"
                                value = "src/main/resources/schema.sql"
                            }

                            // The sort order of the scripts within a directory, where:
                            //
                            // - semantic: sorts versions, e.g. v-3.10.0 is after v-3.9.0 (default)
                            // - alphanumeric: sorts strings, e.g. v-3.10.0 is before v-3.9.0
                            // - flyway: sorts files the same way as flyway does
                            // - none: doesn't sort directory contents after fetching them from the directory
                            property {
                                key = "sort"
                                value = "semantic"
                            }

                            // The default schema for unqualified objects:
                            //
                            // - public: all unqualified objects are located in the PUBLIC (upper case) schema
                            // - none: all unqualified objects are located in the default schema (default)
                            //
                            // This configuration can be overridden with the schema mapping feature
                            property {
                                key = "unqualifiedSchema"
                                value = "none"
                            }

                            // The default name case for unquoted objects:
                            //
                            // - as_is: unquoted object names are kept unquoted
                            // - upper: unquoted object names are turned into upper case (most databases)
                            // - lower: unquoted object names are turned into lower case (e.g. PostgreSQL)
                            property {
                                key = "defaultNameCase"
                                value = "lower"
                            }
                        }
                    }
                    generate {
                        isPojosAsKotlinDataClasses = true // use data classes
                        // Allowing to turn off the feature for to-many join paths (including many-to-many).
                        // The default is true.
                        // see: https://stackoverflow.com/questions/77677549/new-jooq-gradle-plugin-can-not-process-self-reference-relation-correctly/77677816#77677816
                        isImplicitJoinPathsToMany = false
                    }
                    target {
                        packageName = "com.example.demo.jooq"

                        // can not resolve relative path, use
                        // basedir = "${projectDir}"
                        // or append `${projectDir}` to the beginning of the relative path.
                        // see: https://github.com/jOOQ/jOOQ/issues/15944
                        directory = "${projectDir}/build/generated/jooq/main"  // default (can be omitted)
                    }
                    strategy {
                        name = "org.jooq.codegen.DefaultGeneratorStrategy"
                    }
                }
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
