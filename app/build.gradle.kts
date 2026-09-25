import java.util.Properties

plugins {
    id("com.lelloman.paravoid")
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
    id("com.lelloman.paravoid.hilt")
}

val signingProperties = Properties().apply {
    val signingPropertiesFile = rootProject.file("signing.properties")
    if (signingPropertiesFile.exists()) {
        signingPropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "com.lelloman.accordomi"
    compileSdk = 36
    ndkVersion = "27.0.12077973"
    externalNativeBuild {
        cmake {
            path = rootProject.file("native/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    defaultConfig {
        applicationId = "com.lelloman.accordomi"
        minSdk = 29
        targetSdk = 37
        versionCode = 9
        versionName = "1.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake { arguments += "-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON" }
        }
    }

    productFlavors {
        getByName("paravoidAndroid") {
            applicationIdSuffix = ""
            minSdk = 30
        }
    }

    signingConfigs {
        if (signingProperties.containsKey("storeFile")) {
            create("release") {
                storeFile = file(signingProperties.getProperty("storeFile"))
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // AGP shrinking is unsupported for Paravoid; its payload R8 path is configured below.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        create("storeRelease") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = true
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

androidComponents {
    beforeVariants(selector().withBuildType("storeRelease")) { variant ->
        if (variant.productFlavors.any { it.second == "paravoidAndroid" }) {
            variant.enable = false
        }
    }
}

kapt {
    correctErrorTypes = true
}

paravoid {
    packaging = "complete"
    bootstrap = "embedded"
    controlsLauncher = false
    crashRecovery {
        enabled = true
        updater = "default"
    }
    minifyPayload = providers.gradleProperty("paravoidMinifyPayload").map(String::toBoolean).getOrElse(false)
    payloadProguardFiles.from("proguard-rules.pro")
    payloadVersion = providers.gradleProperty("paravoidPayloadVersion").map(String::toLong).getOrElse(14L)
    providers.gradleProperty("paravoidBaselineDirectory").orNull?.let {
        baselineDirectory.set(layout.dir(providers.provider { file(it) }))
    }
    signing {
        keyId = providers.environmentVariable("PARAVOID_SIGNING_KEY_ID").getOrElse("accordomi-v1")
        privateKeyFile.set(layout.file(providers.environmentVariable("PARAVOID_SIGNING_KEY").map(::file)))
    }
    updates {
        val updateUrl = providers.environmentVariable("PARAVOID_UPDATE_BASE_URL").orNull.orEmpty()
        enabled = updateUrl.isNotBlank()
        authentication = "apkKey"
        baseUrl = updateUrl
        schedule {
            checks = false
            downloads = false
        }
        push {
            enabled = updateUrl.isNotBlank()
            webSocketUrl = updateUrl.replaceFirst("https://", "wss://").trimEnd('/') + "/v1/events"
            behavior = "automatic"
        }
        trustPolicyFile.set(layout.file(providers.environmentVariable("PARAVOID_TRUST_POLICY").map(::file)))
    }
}

dependencies {
    implementation(project(":paravoid-api"))
    add("paravoidAndroidImplementation", project(":paravoid-runtime"))
    implementation("com.lelloman:lellodesign-compose:0.2.1")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt(libs.hilt.android.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// JVM regression tests load a host build of the same JNI/C sources used by Android.
val hostNativeDirectory = rootProject.layout.buildDirectory.dir("native-jvm")
val configureHostNative = tasks.register<Exec>("configureHostNative") {
    inputs.files(rootProject.fileTree("native") { exclude("README.md") })
    outputs.file(hostNativeDirectory.map { it.file("CMakeCache.txt") })
    commandLine("cmake", "-S", rootProject.file("native").absolutePath,
        "-B", hostNativeDirectory.get().asFile.absolutePath,
        "-DAC_BUILD_JNI=ON", "-DCMAKE_BUILD_TYPE=Release")
}
val buildHostNative = tasks.register<Exec>("buildHostNative") {
    dependsOn(configureHostNative)
    inputs.files(rootProject.fileTree("native") { exclude("README.md") })
    outputs.file(hostNativeDirectory.map { it.file(System.mapLibraryName("accordomi_jni")) })
    commandLine("cmake", "--build", hostNativeDirectory.get().asFile.absolutePath,
        "--target", "accordomi_jni", "--parallel", "2")
}
tasks.withType<Test>().configureEach {
    dependsOn(buildHostNative)
    inputs.file(hostNativeDirectory.map { it.file(System.mapLibraryName("accordomi_jni")) })
    systemProperty("java.library.path", hostNativeDirectory.get().asFile.absolutePath)
}
