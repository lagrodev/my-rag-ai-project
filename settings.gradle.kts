rootProject.name = "my-rag-ai-project"
include("app")
include("common:api-contracts")
include("common:core-utils")
include("features:rag-core")       // Бывший knowledge-service
include("features:document-manager")