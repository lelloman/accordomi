pluginManagement {
    val paravoidDirectory = providers.environmentVariable("PARAVOID_SOURCE_DIRECTORY")
        .getOrElse("../paravoid-android")
    includeBuild("$paravoidDirectory/paravoid-gradle-plugin")
    includeBuild("$paravoidDirectory/paravoid-hilt")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven {
                    name = "Fucina"
                    url = uri("https://fucina.homelab/api/packages/lelloman/maven")
                    credentials {
                        username = "lelloman"
                        password = providers.environmentVariable("FUCINA_MAVEN_TOKEN").orNull
                            ?: providers.environmentVariable("FUCINA_MAVEN_TOKEN_FILE").orNull
                                ?.let { file(it).takeIf { f -> f.isFile }?.readText()?.trim() }
                            ?: file(System.getProperty("user.home") + "/.config/fucina/lellodesign-maven-consume.token")
                                .takeIf { it.isFile }?.readText()?.trim()
                    }
                }
            }
            filter { includeModule("com.lelloman", "lellodesign-compose") }
        }
    }
}

rootProject.name = "Accordomi"
include(":app")
include(":paravoid-api", ":paravoid-contract", ":paravoid-runtime", ":paravoid-recovery-api")
val paravoidDirectory = file(providers.environmentVariable("PARAVOID_SOURCE_DIRECTORY")
    .getOrElse("../paravoid-android"))
// Older installed shells predate the standalone update API.
if (paravoidDirectory.resolve("paravoid-update-api").isDirectory) {
    include(":paravoid-update-api")
    project(":paravoid-update-api").projectDir = paravoidDirectory.resolve("paravoid-update-api")
}
project(":paravoid-api").projectDir = paravoidDirectory.resolve("paravoid-api")
project(":paravoid-contract").projectDir = paravoidDirectory.resolve("paravoid-contract")
project(":paravoid-runtime").projectDir = paravoidDirectory.resolve("paravoid-runtime")

project(":paravoid-recovery-api").projectDir = paravoidDirectory.resolve("paravoid-recovery-api")
