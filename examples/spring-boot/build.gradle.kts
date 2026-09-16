plugins {
    java
    id("org.springframework.boot") version "3.5.16"
}

group = "com.example"
version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenLocal()      // notifyhub-spring-boot-starter 由根工程 publishNotifyHubToMavenLocal 装到这里
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.16"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")   // /actuator/health + /actuator/metrics
    implementation("io.notifyhub:notifyhub-spring-boot-starter:0.1.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
