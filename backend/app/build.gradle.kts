import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
    java
    idea
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ai.chat"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


the<DependencyManagementExtension>().apply {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}



dependencies {

    // Подключаем наши фичи
    implementation(project(":features:rag-core"))
    implementation(project(":features:document-manager"))

    // Подключаем общие штуки (если нужны в main)
    implementation(project(":common:core-utils"))

    // minIo
    implementation("io.minio:minio:8.6.0")
    // mapstruct
    implementation("org.mapstruct:mapstruct:1.7.0.Beta1")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.7.0.Beta1")


    // Web dependencies
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Security dependencies
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.bouncycastle:bcprov-jdk18on:1.83")
    implementation("org.jspecify:jspecify:1.0.0")

    // Database dependencies
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Testing dependencies
    testImplementation("org.springframework.security:spring-security-test")

    // Annotation Processors
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // mapstruct lombok binding
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform()
}