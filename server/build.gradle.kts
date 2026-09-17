plugins {
    `java-library`
    application
}

group = "io.notifyhub"
version = "0.1.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":protos"))
    implementation("io.grpc:grpc-netty-shaded:1.79.0")
    implementation("org.yaml:snakeyaml:2.7")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.2")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.slf4j:slf4j-simple:2.0.17")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.wiremock:wiremock-standalone:3.9.1")
    testImplementation(project(":sdk-java"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "io.notifyhub.Main"
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        showStandardStreams = false
    }
}
