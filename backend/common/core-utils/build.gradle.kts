plugins {
    id("java-library")
}

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")



    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    api("jakarta.persistence:jakarta.persistence-api")
    api("org.springframework.data:spring-data-jpa")
    implementation("org.hibernate.orm:hibernate-core")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}