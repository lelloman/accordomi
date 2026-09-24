pluginManagement {
    includeBuild("../paravoid-android/paravoid-gradle-plugin")
    includeBuild("../paravoid-android/paravoid-hilt")
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
project(":paravoid-api").projectDir = file("../paravoid-android/paravoid-api")
project(":paravoid-contract").projectDir = file("../paravoid-android/paravoid-contract")
project(":paravoid-runtime").projectDir = file("../paravoid-android/paravoid-runtime")

project(":paravoid-recovery-api").projectDir = file("../paravoid-android/paravoid-recovery-api")
