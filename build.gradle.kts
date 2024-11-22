import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    alias(libs.plugins.errorProne)
    alias(libs.plugins.checkerFramework)
    alias(libs.plugins.spotless)
    alias(libs.plugins.mavenPublish)
    jacoco
}

repositories { mavenCentral() }

group = "io.github.iyanging"

dependencies {
    api("org.jspecify:jspecify:${libs.versions.jspecify.get()}")

    compileOnly("com.google.errorprone:error_prone_check_api:${libs.versions.errorProne.get()}")
    compileOnly(
        "com.google.auto.service:auto-service-annotations:${libs.versions.autoService.get()}"
    )

    annotationProcessor("com.google.auto.service:auto-service:${libs.versions.autoService.get()}")

    checkerFramework("org.checkerframework:checker:${libs.versions.checkerFramework.get()}")
    errorprone("com.google.errorprone:error_prone_core:${libs.versions.errorProne.get()}")

    testImplementation(
        "com.google.errorprone:error_prone_test_helpers:${libs.versions.errorProne.get()}"
    )

    testRuntimeOnly("jakarta.persistence:jakarta.persistence-api:${libs.versions.jpaApi.get()}")
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

val javacExports =
    listOf(
        "--add-exports=java.base/jdk.internal.javac=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
    )

tasks.withType<JavaCompile>().all {
    options.compilerArgs.addAll(javacExports)

    options.errorprone {
        disableWarningsInGeneratedCode = true
        errorproneArgs =
            listOf(
                "-XepAllSuggestionsAsWarnings",
            )
        checks =
            mapOf(
                "ReferenceEquality" to CheckSeverity.ERROR,
                "UnnecessaryParentheses" to CheckSeverity.OFF,
                "MisformattedTestData" to CheckSeverity.OFF,
            )
    }
}

tasks.withType<Test>().all {
    jvmArgs(javacExports)

    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)

        exceptionFormat = TestExceptionFormat.FULL
    }
}

checkerFramework {
    checkers =
        listOf(
            "org.checkerframework.checker.nullness.NullnessChecker",
        )
    extraJavacArgs =
        listOf(
            "-Astubs=$rootDir/typings",
            "-AskipFiles=/build/generated/",
            "-AstubNoWarnIfNotFound",
            "-AwarnUnneededSuppressions",
        )
    excludeTests = true
}

spotless {
    java {
        target("**/*.java")
        targetExclude("build/")
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
        indentWithSpaces(2)
        trimTrailingWhitespace()
        endWithNewline()
    }
    json {
        target("**/*.json")
        gson().indentWithSpaces(2)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

mavenPublishing {
    configure(JavaLibrary(javadocJar = JavadocJar.Empty(), sourcesJar = true))

    pom {
        url = "https://github.com/iyanging/error-prone-asis"
        description = "Error Prone extended checks keep code as-is to reflect your intentions"
        licenses {
            license {
                name = "Mulan Permissive Software License v2"
                url = "https://license.coscl.org.cn/MulanPSL2"
            }
        }
        developers {
            developer {
                id = "iyanging"
                name = "iyanging"
                url = "https://github.com/iyanging/"
            }
        }
        scm { url = "https://github.com/iyanging/error-prone-asis" }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)

    signAllPublications()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required = true
        csv.required = false
    }
}

tasks.check { dependsOn(tasks.jacocoTestReport) }
