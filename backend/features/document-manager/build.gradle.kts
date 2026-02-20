plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies{
    // minIo
    implementation("io.minio:minio:8.6.0")


    // JPA

    api("jakarta.persistence:jakarta.persistence-api")
    api("org.springframework.data:spring-data-jpa")
    implementation("org.hibernate.orm:hibernate-core")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")


    // kafka
    implementation("org.springframework.kafka:spring-kafka")

    implementation(project(":common:core-utils")) // Если там есть какие-то утилиты
    // Tika


    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Annotation Processors
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    //  langchain4j
    implementation("dev.langchain4j:langchain4j:1.11.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.11.0")

    // mapstruct lombok binding
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
}
