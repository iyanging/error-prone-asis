import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    base
    `jacoco-report-aggregation`
    alias(libs.plugins.spotless)
    alias(libs.plugins.licenser)
    alias(libs.plugins.errorProne) apply false
    alias(libs.plugins.nullaway) apply false
}

repositories { mavenCentral() }

group = "io.github.iyanging"

subprojects {
    apply(plugin = rootProject.libs.plugins.licenser.get().pluginId)

    group = rootProject.group

    repositories { mavenCentral() }

    plugins.withType<JavaPlugin> {
        apply(plugin = rootProject.libs.plugins.errorProne.get().pluginId)
        apply(plugin = rootProject.libs.plugins.nullaway.get().pluginId)

        extensions.getByType<JavaPluginExtension>().apply {
            toolchain { languageVersion = JavaLanguageVersion.of(21) }
        }

        dependencies {
            val errorprone = configurations["errorprone"]

            errorprone(libs.errorProneCore)
            errorprone(libs.nullaway)
        }

        tasks.withType<JavaCompile> {
            options.errorprone {
                disableWarningsInGeneratedCode = true
                errorproneArgs = listOf("-XepAllSuggestionsAsWarnings")
                checks =
                    mapOf(
                        "ReferenceEquality" to CheckSeverity.ERROR,
                        "UnnecessaryParentheses" to CheckSeverity.OFF,
                        "MisformattedTestData" to CheckSeverity.OFF,
                    )

                nullaway {
                    error()
                    annotatedPackages.add(project.group.toString())
                }
            }
        }

        tasks.withType<JacocoReport>().forEach {
            it.reports { xml.required = true }
            tasks.check { dependsOn(it) }
        }
    }

    tasks.withType<Test>().all {
        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)

            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    plugins.withType<JacocoPlugin> { rootProject.dependencies { jacocoAggregation(project) } }

    license { rule(file("$rootDir/configs/license-template.txt")) }

    plugins.withType<BasePlugin> { tasks.check { dependsOn(tasks.checkLicenses) } }
}

val asis =
    project("error-prone-asis") {
        plugins.withType<JavaPlugin> {
            dependencies {
                val annotationProcessor = configurations["annotationProcessor"]

                annotationProcessor(project(":error-prone-asis-docgen"))
            }
        }
    }

tasks.register<Copy>("generateDocs") {
    group = "documentation"

    dependsOn(asis.tasks["compileJava"])

    val asisGeneratedSource =
        asis.tasks.named<JavaCompile>("compileJava") {
            options.generatedSourceOutputDirectory.get()
        }

    delete("$rootDir/docs/generated")
    from("${asisGeneratedSource}/docs", "$rootDir/docs")
    into("$rootDir/generated-docs")
}

spotless {
    java {
        target("**/*.java")
        targetExclude("**/build/")
        importOrderFile("$rootDir/configs/eclipse-organize-imports.importorder")
        removeUnusedImports()
        eclipse().configFile("$rootDir/configs/eclipse-code-formatter.xml")
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktfmt("0.50").kotlinlangStyle().configure {
            it.setBlockIndent(4)
            it.setContinuationIndent(4)
            it.setRemoveUnusedImports(true)
        }
    }
    yaml {
        target("**/*.yaml", "**/*.yml")
        targetExclude("**/.venv/")
        leadingTabsToSpaces(2)
        trimTrailingWhitespace()
        endWithNewline()
    }
    json {
        target("**/*.json")
        targetExclude("**/.venv/")
        gson().indentWithSpaces(2)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

reporting {
    reports {
        @Suppress("UnstableApiUsage")
        create<JacocoCoverageReport>("testCodeCoverageReport") { testSuiteName = "test" }
    }
}

tasks.check { dependsOn(tasks["testCodeCoverageReport"]) }
