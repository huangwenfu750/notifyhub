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
}

repositories {
    mavenCentral()
}

dependencies {
    // 供 server 与各 SDK 共用的生成代码；版本与服务端对齐
    api("com.google.protobuf:protobuf-java:4.36.1")
    api("io.grpc:grpc-stub:1.79.0")
    api("io.grpc:grpc-protobuf:1.79.0")
    compileOnly("org.apache.tomcat:annotations-api:6.0.53")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("NotifyHub protos")
                description.set("NotifyHub gRPC 生成代码（proto 契约的 Java stub）")
                url.set("https://github.com/huangwenfu750/notifyhub")
            }
        }
    }
}
