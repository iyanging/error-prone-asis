import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    `java-library`
    `maven-publish`
    signing
    id("com.diffplug.spotless") version "7.0.0.BETA3"
}

repositories { mavenCentral() }

group = "io.github.iyanging"

dependencies {
    compileOnly("com.google.errorprone:error_prone_check_api:2.35.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")

    annotationProcessor("com.google.auto.service:auto-service:1.1.1")

    testImplementation("com.google.errorprone:error_prone_test_helpers:2.35.1")

    testRuntimeOnly("jakarta.persistence:jakarta.persistence-api:3.2.0")
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

tasks.withType<JavaCompile>().all { options.compilerArgs.addAll(javacExports) }

tasks.withType<Test>().all {
    jvmArgs(javacExports)

    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)

        exceptionFormat = TestExceptionFormat.FULL
    }
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

publishing {
    publications {
        create<MavenPublication>("errorProneAsis") {
            from(components["java"])
            artifactId = "error_prone_asis"
        }
    }

    repositories {
        maven {
            name = "MavenCentral"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}
