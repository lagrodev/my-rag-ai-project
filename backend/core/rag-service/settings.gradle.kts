rootProject.name = "rag-service"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

include("app")

include("common:api-contracts")
include("common:core-utils")

include("features:rag-core")
include("features:document-manager")
