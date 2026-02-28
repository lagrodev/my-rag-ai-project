plugins {
    id("java-library")
}

dependencies {

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.security)

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.spring.boot.starter.validation)
    api(libs.jakarta.persistence.api)
    api(libs.spring.data.jpa)
    implementation(libs.hibernate.core)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}