import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
    java
    idea
    alias(libs.plugins.springBoot) apply false
    alias(libs.plugins.dependencyManagement) apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "idea")

    the<DependencyManagementExtension>().apply {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
        }
    }
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(rootProject.libs.versions.java.get())
        }
    }

    configurations {
        compileOnly {
            extendsFrom(configurations.annotationProcessor.get())
        }
    }

    dependencies {
        // Annotation Processors
        compileOnly(rootProject.libs.lombok)
        annotationProcessor(rootProject.libs.lombok)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    group = "ai.chat.backend"
    version = "0.0.1-SNAPSHOT"
}

tasks.test {
    useJUnitPlatform()
}