import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import net.ltgt.gradle.nullaway.nullaway
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    alias(libs.plugins.errorProne)
    alias(libs.plugins.nullaway)
    alias(libs.plugins.licenser)
    alias(libs.plugins.spotless)
    `jacoco-report-aggregation`
}

repositories { mavenCentral() }

dependencies {
    errorprone(libs.errorProneCore)
    errorprone(libs.nullaway)

    subprojects.forEach { sp ->
        if (sp.plugins.hasPlugin(JacocoPlugin::class)) {
            jacocoAggregation(sp)
        }
    }
}

group = "io.github.iyanging"

subprojects {
    apply(plugin = rootProject.libs.plugins.errorProne.get().pluginId)
    apply(plugin = rootProject.libs.plugins.nullaway.get().pluginId)
    apply(plugin = rootProject.libs.plugins.licenser.get().pluginId)

    group = rootProject.group

    repositories { mavenCentral() }

    plugins.withType<JavaPlugin> {
        java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

        dependencies {
            errorprone(libs.errorProneCore)
            errorprone(libs.nullaway)
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

                nullaway {
                    error()
                    annotatedPackages.add(project.group.toString())
                }
            }
        }
    }

    tasks.withType<Test>().all {
        testLogging {
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)

            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    license { rule(file("$rootDir/configs/license-template.txt")) }

    plugins.withType<BasePlugin> { tasks.check { dependsOn(tasks.checkLicenses) } }

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
