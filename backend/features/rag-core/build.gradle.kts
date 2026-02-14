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
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.15.2")
    implementation(project(":common:core-utils"))
    api("jakarta.persistence:jakarta.persistence-api")
    api("org.springframework.data:spring-data-jpa")
    implementation("org.hibernate.orm:hibernate-core")

    implementation("org.apache.tika:tika-core:3.2.3")
    implementation("org.apache.tika:tika-parsers-standard-package:3.2.3")
    //  langchain4j
    implementation("dev.langchain4j:langchain4j:1.11.0")
    implementation("dev.langchain4j:langchain4j-open-ai:1.11.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
