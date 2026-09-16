rootProject.name = "notifyhub"

include("protos", "server")
include("sdk-java")
project(":sdk-java").projectDir = file("sdks/java")
include("sdk-spring-boot")
project(":sdk-spring-boot").projectDir = file("sdks/spring-boot")
