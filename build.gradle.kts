plugins {
    base
    id("java-library")
    id("io.freefair.lombok") version "9.1.0"
    id("com.diffplug.spotless") version "8.2.1"
}

repositories {
    mavenCentral()
    mavenLocal()
}

allprojects {
    group = "gg.itzkatze"
    version = "0.0.1-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "com.diffplug.spotless")

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    dependencies {
        implementation("org.reflections:reflections:0.10.2")
        implementation("com.google.code.gson:gson:2.13.2")
        implementation("net.kyori:adventure-platform-bukkit:4.4.1")

        compileOnly("io.papermc.paper:paper-api:26.1.2.build.74-stable")

        compileOnly("org.projectlombok:lombok:1.18.42")
        compileOnly("org.jetbrains:annotations:26.0.1")
    }

    spotless {
        java {
            target("**/*.java")
            removeUnusedImports()
            toggleOffOn()
            trimTrailingWhitespace()
            formatAnnotations()
        }
    }
}
