plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    application
}

group = "com.apptolast"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // MCP Protocol - implementing ourselves for now
    
    // Ktor Server
    implementation("io.ktor:ktor-server-core:3.0.1")
    implementation("io.ktor:ktor-server-netty:3.0.1")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    
    // Database Drivers
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.2.0")
    implementation("mysql:mysql-connector-java:8.0.33")
    
    // Git Integration
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")
    
    // Process Execution
    implementation("org.apache.commons:commons-exec:1.4.0")
    
    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    
    // Configuration
    implementation("com.typesafe:config:1.4.3")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.mockk:mockk:1.13.12")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("com.apptolast.mcp.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.apptolast.mcp.ApplicationKt"
    }
    
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
