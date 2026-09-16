plugins {
    `java-library`
    `maven-publish`
}

group = "io.notifyhub"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
}

val springBootVersion: String by project

dependencies {
    // Spring 相关依赖全部 compileOnly：运行期由使用方项目的 Spring Boot 版本提供，
    // 这样 starter 可同时兼容 Spring Boot 3.x / 4.x。
    api(project(":sdk-java"))

    compileOnly(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework.boot:spring-boot-actuator")
    compileOnly("org.springframework:spring-context")
    compileOnly("org.slf4j:slf4j-api")
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")
    compileOnly("io.micrometer:micrometer-core")
    annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework.boot:spring-boot-autoconfigure")
    testImplementation("org.springframework.boot:spring-boot-actuator")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework:spring-context")
    testImplementation("org.slf4j:slf4j-api")
    testImplementation("org.assertj:assertj-core")
    testImplementation("io.micrometer:micrometer-core")
    // 端到端测试：在进程内拉起真实 NotifyHub 服务端（:server 不依赖本模块，无循环依赖）
    testImplementation(project(":server"))
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("failed", "skipped") }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "notifyhub-spring-boot-starter"
            from(components["java"])
            pom {
                name.set("NotifyHub Spring Boot Starter")
                description.set("NotifyHub 多语言通知推送服务 - Spring Boot 自动配置")
                url.set("https://github.com/notifyhub/notifyhub")
            }
        }
    }
}
