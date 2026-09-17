import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.plugins.signing.SigningExtension

// 根工程不含源码，只提供聚合任务；各模块自带构建脚本（protos / server / sdk-java / sdk-spring-boot）。

// Maven Central 要求每个 jar 都要配 -sources.jar 与 -javadoc.jar，且全部 GPG 签名。
// javadoc 不挂进 assemble（避免每次 build 都跑），只在发布时按需生成。
// 这段必须排在签名配置之前，否则 javadoc 附件来不及进入签名集合。
subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension>("java") {
            withSourcesJar()
        }
        tasks.named<Javadoc>("javadoc") {
            options.encoding = "UTF-8"
            // JDK 21 的 doclint 会因中文注释/缺失 @param 报错，发布产物不需要这份严格度
            (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
        }
        val javadocJar = tasks.register<Jar>("javadocJar") {
            archiveClassifier.set("javadoc")
            from(tasks.named("javadoc"))
        }
        plugins.withId("maven-publish") {
            extensions.configure<PublishingExtension>("publishing") {
                publications.withType<MavenPublication>().configureEach {
                    artifact(javadocJar)
                }
            }
        }
    }
}

/**
 * 把可被外部工程引用的模块一次性发到本地 Maven 仓库（~/.m2）。
 *
 * 只发 `:sdk-spring-boot` 不够：starter 的 POM 依赖 `sdk-java`，后者又依赖 `protos`，
 * 缺任一模块外部工程都会解析失败。
 */
tasks.register("publishNotifyHubToMavenLocal") {
    group = "publishing"
    description = "发布 protos / sdk-java / notifyhub-spring-boot-starter 到 mavenLocal"
    dependsOn(
        ":protos:publishToMavenLocal",
        ":sdk-java:publishToMavenLocal",
        ":sdk-spring-boot:publishToMavenLocal",
    )
}

/**
 * 发布到远端仓库。地址与凭据**只从环境变量或 -P 读取**，不写进仓库：
 *
 * ```
 * MAVEN_URL=https://... MAVEN_USER=... MAVEN_PASSWORD=... gradle publishNotifyHub
 * # 或：gradle publishNotifyHub -PmavenUrl=... -PmavenUser=... -PmavenPassword=...
 * ```
 */
tasks.register("publishNotifyHub") {
    group = "publishing"
    description = "发布 protos / sdk-java / notifyhub-spring-boot-starter 到 MAVEN_URL 指定的远端仓库"
    dependsOn(
        ":protos:publishAllPublicationsToRemoteRepository",
        ":sdk-java:publishAllPublicationsToRemoteRepository",
        ":sdk-spring-boot:publishAllPublicationsToRemoteRepository",
    )
}

/**
 * 发 Maven Central 用：Central Portal 要求一次提交完整的 deployment，
 * 所以先发到一个本地目录（标准 Maven 布局），再整体打成 zip 上传。
 */
val centralBundleDir = layout.buildDirectory.dir("central-bundle").get().asFile

val centralBundlePublishTasks = listOf(
    ":protos:publishAllPublicationsToCentralBundleRepository",
    ":sdk-java:publishAllPublicationsToCentralBundleRepository",
    ":sdk-spring-boot:publishAllPublicationsToCentralBundleRepository",
)

tasks.register<Zip>("centralBundleZip") {
    group = "publishing"
    description = "把 build/central-bundle 打成上传用的 zip"
    dependsOn(centralBundlePublishTasks)          // 先发布到目录，打包时才不会漏文件
    from(centralBundleDir)
    archiveFileName.set("notifyhub-${project(":protos").version}-central-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("central"))
}

tasks.register("publishNotifyHubToCentralBundle") {
    group = "publishing"
    description = "发布到 build/central-bundle 并打成 zip，供 Maven Central Portal 上传"
    dependsOn("centralBundleZip")
}

// 可选 GPG 签名：只在提供了 MAVEN_SIGNING_KEY 时启用（Maven Central 要求，本地构建完全无感）
subprojects {
    plugins.withId("maven-publish") {
        val signingKey = System.getenv("MAVEN_SIGNING_KEY")
        val signingPassword = System.getenv("MAVEN_SIGNING_PASSWORD") ?: ""
        if (!signingKey.isNullOrBlank()) {
            apply(plugin = "signing")
            extensions.configure<SigningExtension>("signing") {
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(extensions.getByType<PublishingExtension>().publications)
            }
        }
    }
}

// 给所有应用了 maven-publish 的模块统一挂远端仓库 + 补全 POM 元信息
subprojects {
    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension>("publishing") {
            val remoteUrl = (System.getenv("MAVEN_URL") ?: findProperty("mavenUrl") as String?)
                ?.takeIf { it.isNotBlank() }
            if (remoteUrl != null) {
                val remoteUser = System.getenv("MAVEN_USER") ?: (findProperty("mavenUser") as String? ?: "")
                val remotePassword = System.getenv("MAVEN_PASSWORD") ?: (findProperty("mavenPassword") as String? ?: "")
                repositories.maven(remoteUrl) {
                    name = "remote"
                    // 匿名仓库（如本地 file:// 测试仓库）不要挂凭据，否则 Gradle 会报协议不支持认证
                    if (remoteUser.isNotBlank()) {
                        credentials {
                            username = remoteUser
                            password = remotePassword
                        }
                    }
                }
            }

            // 打 Central 上传包用的本地目录仓库（Maven 布局）；由 publishNotifyHubToCentralBundle 触发
            repositories.maven(centralBundleDir) {
                name = "centralBundle"
            }

            publications.withType<MavenPublication>().configureEach {
                pom {
                    // 以下元信息是 Maven Central 的要求，但属于项目自身的选择，
                    // 因此从 gradle.properties / -P 读取，仓库里不预置默认值。
                    val licenseName = findProperty("pomLicenseName") as String?
                    if (!licenseName.isNullOrBlank()) {
                        licenses {
                            license {
                                name.set(licenseName)
                                url.set(findProperty("pomLicenseUrl") as String? ?: "")
                            }
                        }
                    }
                    val scmUrl = findProperty("pomScmUrl") as String?
                    if (!scmUrl.isNullOrBlank()) {
                        scm {
                            url.set(scmUrl)
                            connection.set(findProperty("pomScmConnection") as String? ?: "")
                            developerConnection.set(findProperty("pomScmDevConnection") as String? ?: "")
                        }
                    }
                    val devId = findProperty("pomDeveloperId") as String?
                    if (!devId.isNullOrBlank()) {
                        developers {
                            developer {
                                id.set(devId)
                                name.set(findProperty("pomDeveloperName") as String? ?: devId)
                                email.set(findProperty("pomDeveloperEmail") as String? ?: "")
                            }
                        }
                    }
                }
            }
        }
    }
}
