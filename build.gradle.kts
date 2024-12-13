import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    alias(libs.plugins.errorProne)
    alias(libs.plugins.checkerFramework)
    alias(libs.plugins.spotless)
    `jacoco-report-aggregation`
}

repositories { mavenCentral() }

dependencies {
    errorprone(libs.errorProneCore)
    checkerFramework(libs.checkerFramework)

    subprojects.forEach { sp ->
        if (sp.plugins.hasPlugin(JacocoPlugin::class)) {
            jacocoAggregation(sp)
        }
    }
}

subprojects {
    apply(plugin = rootProject.libs.plugins.errorProne.get().pluginId)
    apply(plugin = rootProject.libs.plugins.checkerFramework.get().pluginId)

    group = "io.github.iyanging"

    repositories { mavenCentral() }

    plugins.withType<JavaPlugin> {
        java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

        dependencies {
            errorprone(libs.errorProneCore)
            checkerFramework(libs.checkerFramework)
        }

        tasks.withType<JavaCompile>().all {
            options.errorprone {
                disableWarningsInGeneratedCode = true
                errorproneArgs = listOf("-XepAllSuggestionsAsWarnings")
                checks =
                    mapOf(
                        "ReferenceEquality" to CheckSeverity.ERROR,
                        "UnnecessaryParentheses" to CheckSeverity.OFF,
                        "MisformattedTestData" to CheckSeverity.OFF,
                    )
            }
        }

        checkerFramework {
            checkers = listOf("org.checkerframework.checker.nullness.NullnessChecker")
            extraJavacArgs =
                listOf(
                    "-Astubs=$rootDir/typings",
                    "-AskipFiles=/build/generated/",
                    "-AstubNoWarnIfNotFound",
                    "-AwarnUnneededSuppressions",
                )
            excludeTests = true
        }
    }

    tasks.withType<Test>().all {
        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)

            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    tasks.withType<JacocoReport>().all { reports { xml.required = true } }
}

val asis =
    project("error-prone-asis") {
        plugins.withType<JavaPlugin> {
            dependencies { annotationProcessor(project(":error-prone-asis-docgen")) }
        }
    }

val generateDocs =
    tasks.register<Copy>("generateDocs") {
        group = "documentation"

        dependsOn(asis.tasks.compileJava)

        val asisGeneratedSource =
            asis.tasks.compileJava.get().options.generatedSourceOutputDirectory.get()

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
        indentWithSpaces(2)
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

tasks.check { dependsOn(tasks.jacocoTestReport) }
