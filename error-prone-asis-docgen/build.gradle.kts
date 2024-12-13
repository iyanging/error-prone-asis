plugins { `java-library` }

dependencies {
    api(libs.jspecify)

    compileOnly(libs.errorProneCheckApi)
    compileOnly(libs.autoServiceAnnotations)

    annotationProcessor(libs.autoService)
}

val javacExports =
    listOf(
        "--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
        "--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
    )

tasks.withType<JavaCompile>().all { options.compilerArgs.addAll(javacExports) }
