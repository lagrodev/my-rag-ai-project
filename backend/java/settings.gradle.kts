rootProject.name = "backend"



pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://packages.confluent.io/maven/")
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://packages.confluent.io/maven/")
    }
}

include(":java")
include("java:core")
include("java:core:logging-common")
include("java:core:rag-service")
include("java:core:rag-service:app")
include("java:core:rag-service:common:api-contracts")
include("java:core:rag-service:common:core-utils")
include("java:core:rag-service:features:rag-core")
include("java:core:rag-service:features:document-manager")