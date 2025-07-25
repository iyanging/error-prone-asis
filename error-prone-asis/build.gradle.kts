import java.net.URI

plugins {
    `java-library`
    jacoco
    signing
    `maven-publish`
}

dependencies {
    api(libs.jspecify)

    compileOnly(libs.errorProneCheckApi)
    compileOnly(libs.autoServiceAnnotations)

    annotationProcessor(libs.autoService)

    testImplementation(libs.errorProneTestHelpers)

    testRuntimeOnly(libs.jakartaPersistenceApi)
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

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])

            pom {
                url = "https://github.com/iyanging/error-prone-asis"
                description =
                    "Error Prone extended checks keep code as-is to reflect your intentions"
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
        }
    }

    repositories {
        maven {
            name = "MavenCentral"
            url =
                URI.create(
                    "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"
                )

            credentials {
                val mavenCentralUsername: String? by project
                val mavenCentralPassword: String? by project

                username = mavenCentralUsername
                password = mavenCentralPassword
            }
        }
    }
}

signing {
    val signingInMemoryKey: String? by project
    val signingInMemoryKeyPassword: String? by project

    useInMemoryPgpKeys(signingInMemoryKey, signingInMemoryKeyPassword)

    sign(publishing.publications)
}
