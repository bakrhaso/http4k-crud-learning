import org.gradle.api.JavaVersion.VERSION_21
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
	kotlin("plugin.serialization") version "1.9.23"
    application

	id("org.graalvm.buildtools.native") version "0.10.1"
}

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
    }
}

application {
    mainClass = "com.example.HelloWorldKt"
}

repositories {
    mavenCentral()
}

apply(plugin = "kotlin")

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            allWarningsAsErrors = false
            jvmTarget = "21"
            freeCompilerArgs += "-Xjvm-default=all"
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    java {
        sourceCompatibility = VERSION_21
        targetCompatibility = VERSION_21
    }
}

graalvmNative {
	toolchainDetection.set(true)
	binaries {
		named("main") {
			imageName.set("helloworld")
			mainClass.set("com.example.HelloWorldKt")
			useFatJar.set(true)
		}
	}
}

val http4kVersion: String by project
val junitVersion: String by project
val kotlinVersion: String by project
val exposedVersion: String by project

dependencies {
	// jetbrains
	implementation("org.jetbrains.kotlin:kotlin-stdlib:${kotlinVersion}")

	// http4k
    implementation("org.http4k:http4k-contract:${http4kVersion}")
    implementation("org.http4k:http4k-core:${http4kVersion}")
    implementation("org.http4k:http4k-format-jackson:${http4kVersion}")

	// exposed orm
	implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
	implementation("org.jetbrains.exposed:exposed-java-time:${exposedVersion}")
	implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")

	// postgres driver
	implementation("org.postgresql:postgresql:42.7.3")

	// db connection pool
	implementation("com.zaxxer:HikariCP:4.0.3")

	// test
    testImplementation("org.http4k:http4k-testing-approval:${http4kVersion}")
    testImplementation("org.http4k:http4k-testing-hamkrest:${http4kVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

