plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    application
}

application {
    mainClass.set("com.telegrammer.server.ApplicationKt")
}

ktor {
    fatJar {
        archiveFileName.set("telegrammer-server-all.jar")
    }
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback)
    implementation(libs.mongodb.driver)
    implementation(libs.bson.kotlinx)
    implementation(libs.java.jwt)
    implementation(libs.kotlinx.serialization.json)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
