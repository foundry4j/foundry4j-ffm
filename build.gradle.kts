plugins {
    java
    jacoco
    id("com.vanniktech.maven.publish") version "0.34.0"
    id("org.sonarqube") version "7.5.0.8588"
}

group = "io.github.foundry4j"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(27)
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-parameters"))
}

jacoco {
    toolVersion = "0.8.15"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

sonar {
    properties {
        property("sonar.projectKey", "foundry4j_foundry4j-ffm")
        property("sonar.organization", "foundry4j")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
    }
}

tasks.matching { it.name == "sonar" }.configureEach {
    dependsOn(tasks.jacocoTestReport)
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    pom {
        name.set("foundry4j-ffm")
        description.set(
            "Zero-dependency, Java-native external memory utilities built on the " +
                "java.lang.foreign module of Java 27. Offers simplified arena pooling, " +
                "typed primitive access helpers, native C string allocation and " +
                "conversion, struct layout builders, and ByteBuffer bridges."
        )
        inceptionYear.set("2026")
        url.set("https://github.com/foundry4j/foundry4j-ffm")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("tuyucheng777")
                name.set("tuyucheng777")
                email.set("925343663@qq.com")
            }
        }
        scm {
            url.set("https://github.com/foundry4j/foundry4j-ffm")
            connection.set("scm:git:git://github.com/foundry4j/foundry4j-ffm.git")
            developerConnection.set("scm:git:ssh://github.com:foundry4j/foundry4j-ffm.git")
        }
    }
}

tasks.matching { it.name == "publishAllPublicationsToMavenCentralRepository" }.configureEach {
    doFirst {
        val missing = listOf(
            "mavenCentralUsername",
            "mavenCentralPassword",
            "signingInMemoryKey",
            "signingInMemoryKeyPassword"
        ).filterNot { providers.gradleProperty(it).isPresent }
        check(missing.isEmpty()) {
            "Missing Gradle properties: ${missing.joinToString()}. " +
                "See README.md for setup instructions."
        }
    }
}
