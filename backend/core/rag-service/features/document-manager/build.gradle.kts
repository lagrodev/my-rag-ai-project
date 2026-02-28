plugins {
    `java-library`
}

dependencies {
    // minIo
    implementation(libs.minio)

    // JPA
    api(libs.jakarta.persistence.api)
    api(libs.spring.data.jpa)
    implementation(libs.hibernate.core)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)

    // kafka
    implementation(libs.spring.kafka)

    implementation(project(":core:rag-service:common:core-utils"))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.spring.boot.starter.validation)

    // langchain4j
    implementation(libs.langchain4j)
    implementation(libs.langchain4j.open.ai)

    // Annotation Processors
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
