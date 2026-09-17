plugins {
    `java-library`
    `maven-publish`
}

group = "io.github.huangwenfu750"
version = "0.1.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":protos"))
    // 客户端需要 gRPC 传输层；netty-shaded 为默认选择
    api("io.grpc:grpc-netty-shaded:1.79.0")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    // test 源集里是 SmokeMain（跨进程烟雾测试入口，非 JUnit 测试）
    failOnNoDiscoveredTests = false
}

/** 对运行中的 NotifyHub 执行 Java SDK 烟雾测试: gradle :sdk-java:smoke -Ptarget=... -Ptoken=... */
tasks.register<JavaExec>("smoke") {
    group = "verification"
    description = "Java SDK 烟雾测试（需先启动 NotifyHub 服务端）"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass = "io.notifyhub.sdk.SmokeMain"
    // gradle.properties 的 jvmargs 管不到 JavaExec 派生的 JVM，中文输出会乱码
    jvmArgs("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
    systemProperty("notifyhub.target", (project.findProperty("target") ?: "127.0.0.1:9987") as String)
    systemProperty("notifyhub.token", (project.findProperty("token") ?: "") as String)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("NotifyHub Java SDK")
                description.set("NotifyHub 多语言通知推送服务 - Java 客户端")
                url.set("https://github.com/huangwenfu750/notifyhub")
            }
        }
    }
}
