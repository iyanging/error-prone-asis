import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `java-library`
    jacoco
    alias(libs.plugins.mavenPublish)
}

val sourceSetDocs =
    sourceSets.create("docs") {
        java.srcDirs(sourceSets.main.get().java.srcDirs)
        annotationProcessorPath += sourceSets.main.get().annotationProcessorPath
        compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
        runtimeClasspath += sourceSets.main.get().runtimeClasspath
    }

val docsAnnotationProcessor =
    configurations.named(sourceSetDocs.annotationProcessorConfigurationName).get()

dependencies {
    api(libs.jspecify)

    compileOnly(libs.errorProneCheckApi)
    compileOnly(libs.autoServiceAnnotations)

    annotationProcessor(libs.autoService)

    testImplementation(libs.errorProneTestHelpers)

    testRuntimeOnly(libs.jakartaPersistenceApi)

    docsAnnotationProcessor(project(":asis-docgen"))
}

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

tasks.withType<Test>().all { jvmArgs(javacExports) }

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required = true
        csv.required = false
    }
}

tasks.check { dependsOn(tasks.jacocoTestReport) }

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
