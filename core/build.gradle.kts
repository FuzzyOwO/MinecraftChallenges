plugins {
    id("com.gradleup.shadow") version "9.3.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    api("org.slf4j:slf4j-api:2.0.16")
    api("ch.qos.logback:logback-classic:1.5.25")
}
