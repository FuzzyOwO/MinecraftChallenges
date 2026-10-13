plugins {
    java
    application
    id("com.gradleup.shadow") version "9.3.1"
}

val targetJavaVersion = 25

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":forcebattle"))
    implementation(project(":allitems"))
    implementation(project(":deathswap"))
    implementation(project(":randomizer"))
    implementation(project(":itemrain"))
    compileOnly("io.papermc.paper:paper-api:26.3.build.175-stable")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

tasks.shadowJar {
    mergeServiceFiles()
    archiveBaseName.set("MinecraftChallenges")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
