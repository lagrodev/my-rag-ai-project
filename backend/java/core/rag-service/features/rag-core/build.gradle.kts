plugins {
    `java-library`
}

dependencies {

    // web
    implementation(libs.spring.boot.starter.web)

    implementation(libs.hypersistence.utils)
    implementation(project(":core:rag-service:common:core-utils"))
    implementation(project(":core:logging-common"))
    api(libs.jakarta.persistence.api)
    api(libs.spring.data.jpa)
    implementation(libs.hibernate.core)
    implementation(libs.hibernate.vector)
    implementation(libs.spring.kafka)

    implementation(libs.tika.core)
    implementation(libs.tika.parsers)

    // langchain4j
    implementation(libs.langchain4j)
    implementation(libs.langchain4j.open.ai)

    // redis
    implementation(libs.spring.boot.starter.data.redis)
    compileOnly(libs.jedis)
    // jackson
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
