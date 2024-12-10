# Getting Started

## What is ErrorProne ?

[**Error Prone**](https://errorprone.info/) is a static analysis tool for Java that catches common programming mistakes at compile-time.

## What is ErrorProne-AsIs ?

ErrorProne-AsIs contains extended checks that keep code as-is to reflect your intentions.

## How to install ErrorProne ?

See [ErrorProne - Installation](https://errorprone.info/docs/installation)

## How to install ErrorProne-AsIs ?

Just put it in annotation processor path together with ErrorProne

## Want some Gradle example ?

```kotlin title="build.gradle.kts"
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("net.ltgt.errorprone") version "<version>"
}

dependencies {
    errorprone("com.google.errorprone:error_prone_core:<version>")
    errorprone("io.github.iyanging:error-prone-asis:<version>")
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        disableWarningsInGeneratedCode = true
        errorproneArgs = listOf("-XepAllSuggestionsAsWarnings")
        checks = mapOf(
            "MissingCasesInEnumSwitch" to CheckSeverity.ERROR,
            "ReferenceEquality" to CheckSeverity.ERROR,
            "UnnecessaryParentheses" to CheckSeverity.OFF,
            // error-prone-asis
            "JpaImplicitEnum" to CheckSeverity.ERROR,
            "JpaDefaultDecimal" to CheckSeverity.ERROR,
        )
    }
}
```
