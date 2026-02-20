import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
    id("java-base")
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("org.springframework.boot") version "4.0.1" apply false
}

allprojects {
    group = "ai.chat"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "io.spring.dependency-management")

    configure<DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.1")
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "21" // Или 21
        targetCompatibility = "21"
    }

}


