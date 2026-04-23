plugins {
  id("java")
}

group = "com.github.ieatglu3"
version = "1.0.0"

repositories {
  mavenCentral()
  maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
  maven("https://repo.codemc.io/repository/maven-releases/")
  maven("https://repo.codemc.io/repository/maven-snapshots/")
}

dependencies {
  compileOnly(project(":"))
  compileOnly("org.spigotmc:spigot-api:1.13-R0.1-SNAPSHOT")
  compileOnly("com.github.retrooper:packetevents-api:2.12.0")
  testImplementation(platform("org.junit:junit-bom:5.10.0"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
  useJUnitPlatform()
}