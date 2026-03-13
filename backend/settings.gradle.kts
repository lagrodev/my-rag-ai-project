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

include("core")
include("core:logging-common")
include("core:rag-service")
include("core:rag-service:app")
include("core:rag-service:common:api-contracts")
include("core:rag-service:common:core-utils")
include("core:rag-service:features:rag-core")
include("core:rag-service:features:document-manager")