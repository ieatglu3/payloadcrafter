plugins {
  id("java")
  kotlin("jvm") version "1.9.20"
}

group = "com.github.ieatglu3"
version = "1.0.0"

repositories {
  mavenCentral()
  maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
  maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
}

dependencies {
  compileOnly("com.github.retrooper:packetevents-api:2.12.0")
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
  useJUnitPlatform()
}

allprojects {
  tasks {
    withType<Jar> {
      archiveBaseName = "${rootProject.name}-${project.name}"
    }
  }
}