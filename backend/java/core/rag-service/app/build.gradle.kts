plugins {
    java
    idea
    alias(libs.plugins.springBoot)
}

group = "ai.chat"

dependencies {

    // Подключаем наши фичи
    implementation(project(":core:rag-service:features:rag-core"))
    implementation(project(":core:rag-service:features:document-manager"))

    // Подключаем общие штуки (если нужны в main)
    implementation(project(":core:rag-service:common:core-utils"))
    implementation(libs.spring.kafka)

    implementation(libs.spring.boot.starter.validation)
    // Web dependencies
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.datatype.jsr310)

    // Security dependencies
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.jspecify)

    // Database dependencies
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.spring.boot.starter.validation)
    runtimeOnly(libs.postgresql)
    implementation(libs.spring.boot.starter.data.jpa)

    // Testing dependencies
    testImplementation(libs.spring.security.test)

    // Annotation Processors
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform()
}
