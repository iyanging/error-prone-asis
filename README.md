# ErrorProne-AsIs

Keep code ***as-is*** to reflect your intentions.

## Installation with gradle

```kotlin
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
